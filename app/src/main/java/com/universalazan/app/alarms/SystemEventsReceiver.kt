package com.universalazan.app.alarms

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.universalazan.app.UniversalAzanApp
import kotlinx.coroutines.launch

/**
 * Re-plans alarms after events that clear or invalidate them: phone restart (alarms do not
 * survive a reboot), clock or time-zone change, app update, and exact-alarm permission change.
 * Only future prayer times are scheduled, so nothing missed while the phone was off will play.
 */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val container = (context.applicationContext as UniversalAzanApp).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                container.alarmCoordinator.reschedule()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        // The exact-alarm permission broadcast only exists on Android 12+; older versions never
        // send it, so listing it here is harmless.
        @SuppressLint("InlinedApi")
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
