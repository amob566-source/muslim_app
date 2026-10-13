package com.universalazan.app.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.universalazan.app.UniversalAzanApp
import com.universalazan.app.audio.AzanSound
import com.universalazan.app.domain.prayer.Prayer
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Called by AlarmManager at a prayer time. Starts the Azan (unless the alarm is late, e.g. the
 * phone was off) and then schedules the next alarms. Also receives the once-a-day check.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as UniversalAzanApp).container
        if (intent.action == ACTION_DAILY_CHECK) {
            // Safety net: re-plan once a day in case an earlier re-plan was interrupted.
            val pending = goAsync()
            container.appScope.launch {
                try {
                    container.alarmCoordinator.reschedule()
                } finally {
                    pending.finish()
                }
            }
            return
        }
        if (intent.action != ACTION_PRAYER_ALARM) return
        val prayer = Prayer.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_PRAYER) } ?: return
        val sound = AzanSound.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_SOUND) } ?: AzanSound.Full
        val scheduled = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_TIME, 0L))
        val isTest = intent.getBooleanExtra(EXTRA_TEST, false)
        val now = Instant.now()

        val eventId = "${prayer.name}@${scheduled.toEpochMilli()}"
        when {
            PrayerAlarmPlanner.isMissed(scheduled, now) ->
                Log.i(TAG, "Skipping missed Azan for $prayer (due $scheduled, now $now)")
            !isTest && container.alarmCoordinator.markPlayed(eventId).not() ->
                Log.i(TAG, "Azan for $eventId already handled")
            sound != AzanSound.Silent ->
                // Must start right away: Android only allows this briefly after an exact alarm.
                AzanPlaybackService.start(context, prayer, sound, scheduled.toEpochMilli(), isTest)
        }

        if (isTest) return
        val pending = goAsync()
        container.appScope.launch {
            try {
                container.alarmCoordinator.reschedule()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.universalazan.app.action.PRAYER_ALARM"
        const val ACTION_DAILY_CHECK = "com.universalazan.app.action.DAILY_CHECK"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_TIME = "time"
        const val EXTRA_TEST = "test"
        private const val TAG = "PrayerAlarm"
    }
}
