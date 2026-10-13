package com.universalazan.app.alarms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.universalazan.app.MainActivity
import com.universalazan.app.R
import com.universalazan.app.UniversalAzanApp
import com.universalazan.app.audio.AzanSound
import com.universalazan.app.audio.PlaybackState
import com.universalazan.app.domain.prayer.Prayer
import com.universalazan.app.ui.settings.labelRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Plays an Azan in the background (also with the screen locked) as a short "foreground service",
 * showing a notification with a Stop button. Stops itself when the Azan ends or Stop is pressed.
 */
class AzanPlaybackService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watchJob: Job? = null

    private val player get() = (application as UniversalAzanApp).container.azanPlayer

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            player.stop()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val prayer = Prayer.entries.firstOrNull { it.name == intent?.getStringExtra(EXTRA_PRAYER) }
        val sound = AzanSound.entries.firstOrNull { it.name == intent?.getStringExtra(EXTRA_SOUND) }
        if (prayer == null || sound == null || sound == AzanSound.Silent) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val time = intent?.getLongExtra(EXTRA_TIME, System.currentTimeMillis()) ?: System.currentTimeMillis()
        val isTest = intent?.getBooleanExtra(EXTRA_TEST, false) == true

        // Must be called within a few seconds of starting.
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(prayer, time, isTest),
            // Service types exist from Android 10; earlier versions take 0.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )

        if (!player.play(prayer, sound)) {
            Log.w(TAG, "Azan for $prayer could not play: ${player.state.value}")
            stopSelf(startId)
            return START_NOT_STICKY
        }
        // Finish when this Azan ends (or is stopped). stopSelf(startId) is ignored if a newer
        // Azan has started this service again in the meantime.
        watchJob?.cancel()
        watchJob = scope.launch {
            player.state.first { it !is PlaybackState.Playing }
            ServiceCompat.stopForeground(this@AzanPlaybackService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }
        // If Android kills the service, do not restart it later: a late Azan must not play.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        if (player.state.value is PlaybackState.Playing) player.stop()
        super.onDestroy()
    }

    private fun buildNotification(prayer: Prayer, time: Long, isTest: Boolean): Notification {
        val prayerName = getString(prayer.labelRes)
        val title = if (isTest) getString(R.string.alarm_test_notification_title) else getString(R.string.alarm_notification_title, prayerName)
        val timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(time))
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, AzanPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_prayer_times)
            .setContentTitle(title)
            .setContentText(getString(R.string.alarm_notification_text, timeText))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true) // The Azan itself is the sound.
            .setContentIntent(openApp)
            .setDeleteIntent(stop)
            .addAction(0, getString(R.string.alarm_notification_stop), stop)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val TAG = "AzanPlayback"
        const val CHANNEL_ID = "azan"
        private const val NOTIFICATION_ID = 42
        private const val ACTION_STOP = "com.universalazan.app.action.STOP_AZAN"
        private const val EXTRA_PRAYER = "prayer"
        private const val EXTRA_SOUND = "sound"
        private const val EXTRA_TIME = "time"
        private const val EXTRA_TEST = "test"

        fun start(context: Context, prayer: Prayer, sound: AzanSound, timeMillis: Long, isTest: Boolean) {
            val intent = Intent(context, AzanPlaybackService::class.java)
                .putExtra(EXTRA_PRAYER, prayer.name)
                .putExtra(EXTRA_SOUND, sound.name)
                .putExtra(EXTRA_TIME, timeMillis)
                .putExtra(EXTRA_TEST, isTest)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: IllegalStateException) {
                // Android refused to start playback from the background (for example when exact
                // alarms are not allowed). Nothing can be played; the app explains the fix.
                Log.w(TAG, "Could not start Azan playback", e)
            }
        }

        /** The same request the notification's Stop button sends. */
        fun stop(context: Context) {
            runCatching {
                context.startService(Intent(context, AzanPlaybackService::class.java).setAction(ACTION_STOP))
            }
        }

        /** The notification channel for Azan notifications (no channel sound; the Azan plays itself). */
        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.alarm_channel_description)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
