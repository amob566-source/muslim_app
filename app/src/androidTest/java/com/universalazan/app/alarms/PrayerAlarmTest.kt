package com.universalazan.app.alarms

import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalazan.app.ResetAppSettingsRule
import com.universalazan.app.audio.AzanSound
import com.universalazan.app.audio.PlaybackState
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.domain.prayer.Prayer
import com.universalazan.app.shell
import com.universalazan.app.testAlarmCoordinator
import com.universalazan.app.testAlarmScheduler
import com.universalazan.app.testAlarmSettingsRepository
import com.universalazan.app.testAzanPlayer
import com.universalazan.app.testAzanSoundRepository
import com.universalazan.app.testLocationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The real AlarmManager, receiver and playback service on the emulator or phone. */
@RunWith(AndroidJUnit4::class)
class PrayerAlarmTest {
    @get:Rule
    val reset = ResetAppSettingsRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val packageName = context.packageName
    private val london = SavedLocation("London", 51.5074, -0.1278, "Europe/London", LocationSource.City)
    private val makkah = SavedLocation("Makkah", 21.4266, 39.8256, "Asia/Riyadh", LocationSource.City)

    @Before
    fun grantPermissions() {
        shell("pm grant $packageName android.permission.POST_NOTIFICATIONS")
        shell("appops set $packageName SCHEDULE_EXACT_ALARM allow")
    }

    private fun waitFor(timeoutMs: Long = 5_000, condition: () -> Boolean): Boolean {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < end) {
            if (condition()) return true
            SystemClock.sleep(100)
        }
        return condition()
    }

    /**
     * Number of this app's prayer alarms pending in Android's AlarmManager. Only "tag=" lines
     * are real pending alarms; the dump's history and statistics sections repeat the name.
     */
    private fun registeredPrayerAlarms(): Int =
        shell("dumpsys alarm").lines().count { it.trim() == "tag=*walarm*:${PrayerAlarmReceiver.ACTION_PRAYER_ALARM}" }

    private fun save(location: SavedLocation) = runBlocking { testLocationRepository().save(location) }
    private fun reschedule() = runBlocking { testAlarmCoordinator().reschedule() }

    private fun serviceRunning() = "AzanPlaybackService" in shell("dumpsys activity services $packageName")

    @Test
    fun schedulesOneAlarmPerPrayerAndNoDuplicates() {
        save(london)
        repeat(3) { reschedule() }
        Prayer.entries.forEach { assertTrue("$it not scheduled", testAlarmScheduler().isScheduled(it)) }
        assertEquals(5, registeredPrayerAlarms())
    }

    @Test
    fun usesExactAlarmsWhenAllowed() {
        save(london)
        reschedule()
        assertTrue(testAlarmScheduler().canScheduleExactAlarms())
        assertTrue(shell("dumpsys alarm").contains("exactAllowReason=permission"))
    }

    @Test
    fun silentPrayerAlarmIsRemoved() {
        save(london)
        reschedule()
        runBlocking { testAzanSoundRepository().setSound(Prayer.Asr, AzanSound.Silent) }
        reschedule()
        assertFalse(testAlarmScheduler().isScheduled(Prayer.Asr))
        assertTrue(testAlarmScheduler().isScheduled(Prayer.Dhuhr))
        assertEquals(4, registeredPrayerAlarms())
    }

    @Test
    fun turningAlarmsOffCancelsThemAll() {
        save(london)
        reschedule()
        runBlocking { testAlarmSettingsRepository().setEnabled(false) }
        reschedule()
        Prayer.entries.forEach { assertFalse("$it still scheduled", testAlarmScheduler().isScheduled(it)) }
        assertEquals(0, registeredPrayerAlarms())
    }

    @Test
    fun changingCityReplacesAlarms() {
        save(london)
        val londonPlan = reschedule()
        save(makkah)
        val makkahPlan = reschedule()
        assertNotEquals(londonPlan.map { it.time }, makkahPlan.map { it.time })
        assertEquals(5, registeredPrayerAlarms())
    }

    @Test
    fun settingsChangesRescheduleAutomatically() {
        save(london)
        // No explicit reschedule: the coordinator observes saved settings.
        assertTrue(waitFor { testAlarmCoordinator().upcoming.value.size == 5 })
        runBlocking { testAzanSoundRepository().setSound(Prayer.Isha, AzanSound.Silent) }
        assertTrue(waitFor { testAlarmCoordinator().upcoming.value.none { it.prayer == Prayer.Isha } })
        assertFalse(testAlarmScheduler().isScheduled(Prayer.Isha))
    }

    private fun alarmIntent(prayer: Prayer, sound: AzanSound, timeMillis: Long) =
        Intent(context, PrayerAlarmReceiver::class.java)
            .setAction(PrayerAlarmReceiver.ACTION_PRAYER_ALARM)
            .putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, prayer.name)
            .putExtra(PrayerAlarmReceiver.EXTRA_SOUND, sound.name)
            .putExtra(PrayerAlarmReceiver.EXTRA_TIME, timeMillis)

    @Test
    fun alarmOnTimeStartsTheAzanAndStopWorks() {
        save(london)
        context.sendBroadcast(alarmIntent(Prayer.Maghrib, AzanSound.Full, System.currentTimeMillis()))
        assertTrue("Azan did not start", waitFor { testAzanPlayer().state.value == PlaybackState.Playing(Prayer.Maghrib, AzanSound.Full) })
        assertTrue("service not running", waitFor { serviceRunning() })

        // Same as pressing Stop in the notification.
        AzanPlaybackService.stop(context)
        assertTrue("Azan did not stop", waitFor { testAzanPlayer().state.value == PlaybackState.Idle })
        assertTrue("service still running", waitFor { !serviceRunning() })
    }

    @Test
    fun missedAlarmIsNotPlayed() {
        save(london)
        val tenMinutesAgo = System.currentTimeMillis() - 10 * 60_000
        context.sendBroadcast(alarmIntent(Prayer.Asr, AzanSound.Full, tenMinutesAgo))
        SystemClock.sleep(2_000)
        assertEquals(PlaybackState.Idle, testAzanPlayer().state.value)
        assertFalse(serviceRunning())
    }

    @Test
    fun sameAlarmDeliveredTwicePlaysOnce() {
        save(london)
        val time = System.currentTimeMillis()
        context.sendBroadcast(alarmIntent(Prayer.Isha, AzanSound.Short, time))
        assertTrue(waitFor { testAzanPlayer().state.value is PlaybackState.Playing })
        AzanPlaybackService.stop(context)
        assertTrue(waitFor { testAzanPlayer().state.value == PlaybackState.Idle })

        context.sendBroadcast(alarmIntent(Prayer.Isha, AzanSound.Short, time))
        SystemClock.sleep(2_000)
        assertEquals(PlaybackState.Idle, testAzanPlayer().state.value)
    }

    @Test
    fun firedAlarmReArmsForTheNextDay() {
        save(london)
        val before = reschedule()
        val first = before.first()
        // Simulate that alarm firing late enough to be "missed": it must still re-plan.
        context.sendBroadcast(alarmIntent(first.prayer, first.sound, first.time.toEpochMilli() - 3_600_000))
        assertTrue(waitFor { testAlarmScheduler().isScheduled(first.prayer) })
        assertEquals(5, registeredPrayerAlarms())
    }

    @Test
    fun testAlarmButtonPathPlaysThroughAlarmManager() {
        save(london)
        testAlarmScheduler().scheduleTest(delayMillis = 3_000)
        assertTrue("test alarm did not play", waitFor(timeoutMs = 20_000) { testAzanPlayer().state.value is PlaybackState.Playing })
        AzanPlaybackService.stop(context)
        assertTrue(waitFor { testAzanPlayer().state.value == PlaybackState.Idle })
    }
}
