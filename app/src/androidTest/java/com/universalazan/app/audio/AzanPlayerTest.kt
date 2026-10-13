package com.universalazan.app.audio

import android.media.AudioAttributes
import android.media.AudioManager
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalazan.app.UniversalAzanApp
import com.universalazan.app.domain.prayer.Prayer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real playback on the emulator or phone. Android's list of active audio players is used to
 * check that sound really starts, that only one Azan plays at a time, and that stop works.
 */
@RunWith(AndroidJUnit4::class)
class AzanPlayerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val player = (context.applicationContext as UniversalAzanApp).container.azanPlayer
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    /** Number of this app's players currently playing on the alarm stream. */
    private fun activeAlarmPlayers(): Int = audioManager.activePlaybackConfigurations.count {
        it.audioAttributes.usage == AudioAttributes.USAGE_ALARM
    }

    private fun waitFor(timeoutMs: Long = 5_000, condition: () -> Boolean): Boolean {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < end) {
            if (condition()) return true
            SystemClock.sleep(100)
        }
        return condition()
    }

    @After
    fun tearDown() = onMain { player.stop() }

    @Test
    fun playsAndStops() {
        var started = false
        onMain { started = player.play(Prayer.Dhuhr, AzanSound.Short) }
        assertTrue(started)
        assertEquals(PlaybackState.Playing(Prayer.Dhuhr, AzanSound.Short), player.state.value)
        assertTrue("audio did not start", waitFor { activeAlarmPlayers() == 1 })

        onMain { player.stop() }
        assertEquals(PlaybackState.Idle, player.state.value)
        assertTrue("audio did not stop", waitFor { activeAlarmPlayers() == 0 })
    }

    @Test
    fun startingAnotherSoundNeverPlaysTwoAtOnce() {
        onMain { player.play(Prayer.Fajr, AzanSound.Full) }
        assertTrue(waitFor { activeAlarmPlayers() == 1 })
        onMain { player.play(Prayer.Isha, AzanSound.Full) }
        assertTrue(waitFor { activeAlarmPlayers() >= 1 })
        // Give a duplicate player time to appear if one had been created.
        SystemClock.sleep(1_000)
        assertEquals(1, activeAlarmPlayers())
        assertEquals(PlaybackState.Playing(Prayer.Isha, AzanSound.Full), player.state.value)
    }

    @Test
    fun silentPlaysNothing() {
        var started = true
        onMain { started = player.play(Prayer.Asr, AzanSound.Silent) }
        assertFalse(started)
        SystemClock.sleep(500)
        assertEquals(0, activeAlarmPlayers())
    }

    @Test
    fun everyBundledFileCanBePlayed() {
        listOf(
            Prayer.Fajr to AzanSound.Full,
            Prayer.Maghrib to AzanSound.Full,
            Prayer.Fajr to AzanSound.Short,
        ).forEach { (prayer, sound) ->
            onMain { player.play(prayer, sound) }
            assertTrue("$prayer $sound did not play", waitFor { activeAlarmPlayers() == 1 })
            assertTrue(player.state.value is PlaybackState.Playing)
            onMain { player.stop() }
            assertTrue(waitFor { activeAlarmPlayers() == 0 })
        }
    }

    @Test
    fun shortSoundFinishesOnItsOwn() {
        onMain { player.play(Prayer.Dhuhr, AzanSound.Short) }
        assertTrue(waitFor { activeAlarmPlayers() == 1 })
        // The short Azan is at most about 20 seconds long.
        assertTrue("did not finish", waitFor(timeoutMs = 30_000) { player.state.value == PlaybackState.Idle })
        assertTrue(waitFor { activeAlarmPlayers() == 0 })
    }
}
