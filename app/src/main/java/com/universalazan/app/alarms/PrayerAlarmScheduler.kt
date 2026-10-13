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
 * an obsolete alarm. Separate slots are used for the "test alarm" button and the daily check.
 *
 * Alarms that are still correct are left alone: the scheduler remembers what it last gave
 * Android and only changes a slot whose time, sound or exactness differs. This memory is lost
 * when the app process ends, which is exactly when Android may have cleared alarms anyway
 * (restart, force stop, exact-alarm permission removed), so everything is then set again.
 */
class PrayerAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    private data class SlotState(val triggerAtMillis: Long, val sound: AzanSound?, val exact: Boolean)

    private val applied = mutableMapOf<Int, SlotState>()

    /** How many alarms were actually (re)set or cancelled by the last [apply] (used by tests). */
    @Volatile
    var lastChangeCount: Int = 0
        private set

    /**
     * Exact alarms need the user's permission on Android 12+ (granted in system settings).
     * Android 8-11 have no such permission: exact alarms are always allowed there.
     */
    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager?.canScheduleExactAlarms() == true else true

    /**
     * Makes the AlarmManager match [planned]: schedules those, cancels every other prayer slot.
     * [maintenanceAtMillis] is the daily safety-net check (null to cancel it).
     */
    @Synchronized
    fun apply(planned: List<PlannedAlarm>, maintenanceAtMillis: Long? = null) {
        val exact = canScheduleExactAlarms()
        var changes = 0
        Prayer.entries.forEach { prayer ->
            val code = REQUEST_CODE_BASE + prayer.ordinal
            val alarm = planned.firstOrNull { it.prayer == prayer }
            if (alarm == null) {
                if (applied.remove(code) != null || existing(code, ACTION_PRAYER) != null) {
                    cancel(code, ACTION_PRAYER)
                    changes++
                }
            } else {
                val wanted = SlotState(alarm.time.toEpochMilli(), alarm.sound, exact)
                if (applied[code] != wanted) {
                    schedulePrayer(code, alarm.prayer, alarm.sound, wanted.triggerAtMillis, isTest = false, exact = exact)
                    applied[code] = wanted
                    changes++
                }
            }
        }
        if (maintenanceAtMillis == null) {
            if (applied.remove(MAINTENANCE_REQUEST_CODE) != null || existing(MAINTENANCE_REQUEST_CODE, ACTION_MAINTENANCE) != null) {
                cancel(MAINTENANCE_REQUEST_CODE, ACTION_MAINTENANCE)
            }
        } else {
            // The daily check never needs to be exact; a battery-friendly inexact alarm is enough.
            val wanted = SlotState(maintenanceAtMillis, null, exact = false)
            if (applied[MAINTENANCE_REQUEST_CODE] != wanted) {
                scheduleMaintenance(maintenanceAtMillis)
                applied[MAINTENANCE_REQUEST_CODE] = wanted
            }
        }
        lastChangeCount = changes
    }

    /** Schedules a one-off test Azan (short version) [delayMillis] from now, through the real alarm path. */
    fun scheduleTest(delayMillis: Long, nowMillis: Long = System.currentTimeMillis()) {
        schedulePrayer(TEST_REQUEST_CODE, Prayer.Dhuhr, AzanSound.Short, nowMillis + delayMillis, isTest = true, exact = canScheduleExactAlarms())
    }

    @Synchronized
    fun cancelAll() {
        Prayer.entries.forEach { cancel(REQUEST_CODE_BASE + it.ordinal, ACTION_PRAYER) }
        cancel(TEST_REQUEST_CODE, ACTION_PRAYER)
        cancel(MAINTENANCE_REQUEST_CODE, ACTION_MAINTENANCE)
        applied.clear()
    }

    /** True if the slot for [prayer] currently holds an alarm (used by tests). */
    fun isScheduled(prayer: Prayer): Boolean = existing(REQUEST_CODE_BASE + prayer.ordinal, ACTION_PRAYER) != null

    /** True if the daily check is scheduled (used by tests). */
    fun isMaintenanceScheduled(): Boolean = existing(MAINTENANCE_REQUEST_CODE, ACTION_MAINTENANCE) != null

    private fun schedulePrayer(requestCode: Int, prayer: Prayer, sound: AzanSound, triggerAtMillis: Long, isTest: Boolean, exact: Boolean) {
        val manager = alarmManager ?: return
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
            .setAction(ACTION_PRAYER)
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
        if (exact) {
            // Fires on time even in Doze (battery-saving idle) mode.
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            // Without permission Android may deliver this late; the app explains how to fix it.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun scheduleMaintenance(triggerAtMillis: Long) {
        val manager = alarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MAINTENANCE_REQUEST_CODE,
            Intent(context, PrayerAlarmReceiver::class.java).setAction(ACTION_MAINTENANCE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun cancel(requestCode: Int, action: String) {
        existing(requestCode, action)?.let { alarmManager?.cancel(it); it.cancel() }
    }

    private fun existing(requestCode: Int, action: String): PendingIntent? = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, PrayerAlarmReceiver::class.java).setAction(action),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val REQUEST_CODE_BASE = 1000
        const val MAINTENANCE_REQUEST_CODE = 1098
        const val TEST_REQUEST_CODE = 1099
        private const val ACTION_PRAYER = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
        private const val ACTION_MAINTENANCE = PrayerAlarmReceiver.ACTION_DAILY_CHECK
    }
}
