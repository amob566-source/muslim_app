package com.universalazan.app.audio

import android.content.Context
import android.content.res.Resources
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.annotation.MainThread
import com.universalazan.app.domain.prayer.Prayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackError {
    /** The audio file is missing from the app. */
    MissingAudio,

    /** The file could not be played (damaged or unsupported). */
    PlaybackFailed,

    /** Another app (for example a phone call) has the audio and did not let go. */
    AudioFocusDenied,
}

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data class Playing(val prayer: Prayer, val sound: AzanSound) : PlaybackState
    data class Failed(val prayer: Prayer, val error: PlaybackError) : PlaybackState
}

/**
 * Plays the bundled Azan recordings. One shared instance for the whole app, so two Azans can
 * never play at the same time: starting a new one always stops the previous one first.
 *
 * Audio is streamed from the app's files (not loaded into memory), uses the ALARM volume, and
 * asks other apps to pause while it plays. If a phone call or similar takes the audio, the Azan
 * stops rather than resuming later. Call from the main thread.
 */
class AzanPlayer(private val context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /** True if the alarm volume is turned all the way down, so nothing would be heard. */
    fun isAlarmVolumeMuted(): Boolean = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) == 0

    /**
     * Starts the sound chosen for [prayer]. Returns false if nothing will play (Silent or an
     * error); the reason is in [state].
     */
    @MainThread
    fun play(prayer: Prayer, sound: AzanSound): Boolean {
        stop()
        val resId = azanRawResource(prayer, sound) ?: return false

        val fileDescriptor = try {
            context.resources.openRawResourceFd(resId)
        } catch (_: Resources.NotFoundException) {
            null
        } ?: return fail(prayer, PlaybackError.MissingAudio)

        val mediaPlayer = MediaPlayer()
        try {
            mediaPlayer.setAudioAttributes(attributes)
            fileDescriptor.use { mediaPlayer.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
        } catch (_: Exception) {
            mediaPlayer.release()
            return fail(prayer, PlaybackError.PlaybackFailed)
        }

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stop()
            }
            .build()
        if (audioManager?.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mediaPlayer.release()
            return fail(prayer, PlaybackError.AudioFocusDenied)
        }

        player = mediaPlayer
        focusRequest = request
        mediaPlayer.setOnCompletionListener { if (player === it) stop() }
        mediaPlayer.setOnErrorListener { mp, _, _ ->
            if (player === mp) {
                stop()
                fail(prayer, PlaybackError.PlaybackFailed)
            }
            true
        }
        mediaPlayer.setOnPreparedListener { mp -> if (player === mp) mp.start() }
        _state.value = PlaybackState.Playing(prayer, sound)
        mediaPlayer.prepareAsync()
        return true
    }

    /** Stops any playback and releases the player. Safe to call at any time. */
    @MainThread
    fun stop() {
        player?.let { mp ->
            player = null
            mp.setOnCompletionListener(null)
            mp.setOnErrorListener(null)
            mp.setOnPreparedListener(null)
            runCatching { if (mp.isPlaying) mp.stop() }
            mp.release()
        }
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        focusRequest = null
        if (_state.value is PlaybackState.Playing) _state.value = PlaybackState.Idle
    }

    private fun fail(prayer: Prayer, error: PlaybackError): Boolean {
        _state.value = PlaybackState.Failed(prayer, error)
        return false
    }
}
