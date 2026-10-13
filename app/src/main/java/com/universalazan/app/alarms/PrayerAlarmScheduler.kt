package com.universalazan.app.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.universalazan.app.audio.AzanSound
import com.universalazan.app.domain.prayer.Prayer

/**
 * Talks to Android's AlarmManager.
 *
 * Each prayer has ONE fixed alarm slot (its own request code). Scheduling a prayer replaces
 * whatever was in its slot, so duplicate alarms cannot build up, and cancelling the slot removes
 * an obsolete alarm. A separate slot is used for the "test alarm" button.
 */
class PrayerAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Exact alarms need the user's permission on Android 12+ (granted in system settings).
     * Android 8-11 have no such permission: exact alarms are always allowed there.
     */
    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager?.canScheduleExactAlarms() == true else true

    /** Makes the AlarmManager match [planned]: schedules those, cancels every other prayer slot. */
    fun apply(planned: List<PlannedAlarm>) {
        Prayer.entries.forEach { prayer ->
            val alarm = planned.firstOrNull { it.prayer == prayer }
            if (alarm == null) cancel(prayer) else schedule(alarm.prayer, alarm.sound, alarm.time.toEpochMilli(), isTest = false)
        }
    }

    /** Schedules a one-off test Azan (short version) [delayMillis] from now, through the real alarm path. */
    fun scheduleTest(delayMillis: Long, nowMillis: Long = System.currentTimeMillis()) {
        schedule(Prayer.Dhuhr, AzanSound.Short, nowMillis + delayMillis, isTest = true)
    }

    fun cancelAll() {
        Prayer.entries.forEach(::cancel)
        existing(TEST_REQUEST_CODE)?.let { alarmManager?.cancel(it); it.cancel() }
    }

    private fun schedule(prayer: Prayer, sound: AzanSound, triggerAtMillis: Long, isTest: Boolean) {
        val manager = alarmManager ?: return
        val requestCode = if (isTest) TEST_REQUEST_CODE else REQUEST_CODE_BASE + prayer.ordinal
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
            .setAction(PrayerAlarmReceiver.ACTION_PRAYER_ALARM)
            .putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, prayer.name)
            .putExtra(PrayerAlarmReceiver.EXTRA_SOUND, sound.name)
            .putExtra(PrayerAlarmReceiver.EXTRA_TIME, triggerAtMillis)
            .putExtra(PrayerAlarmReceiver.EXTRA_TEST, isTest)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (canScheduleExactAlarms()) {
            // Fires on time even in Doze (battery-saving idle) mode.
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            // Without permission Android may deliver this late; the app explains how to fix it.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancel(prayer: Prayer) {
        existing(REQUEST_CODE_BASE + prayer.ordinal)?.let { alarmManager?.cancel(it); it.cancel() }
    }

    private fun existing(requestCode: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, PrayerAlarmReceiver::class.java).setAction(PrayerAlarmReceiver.ACTION_PRAYER_ALARM),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )

    /** True if the slot for [prayer] currently holds an alarm (used by tests). */
    fun isScheduled(prayer: Prayer): Boolean = existing(REQUEST_CODE_BASE + prayer.ordinal) != null

    companion object {
        const val REQUEST_CODE_BASE = 1000
        const val TEST_REQUEST_CODE = 1099
    }
}
