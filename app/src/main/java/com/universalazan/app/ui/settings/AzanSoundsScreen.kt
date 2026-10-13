package com.universalazan.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalazan.app.R
import com.universalazan.app.audio.AzanSound
import com.universalazan.app.audio.AzanSoundSettings
import com.universalazan.app.audio.PlaybackError
import com.universalazan.app.audio.PlaybackState
import com.universalazan.app.domain.prayer.Prayer
import com.universalazan.app.ui.common.ScreenContainer

@Composable
fun AzanSoundsRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AzanSoundsViewModel = viewModel(factory = AzanSoundsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val alarmMuted = remember(playback) { viewModel.isAlarmVolumeMuted() }

    BackHandler(onBack = onClose)
    // A test sound stops when the user leaves this screen (but not when the screen rotates).
    DisposableEffect(viewModel) {
        onDispose { if (activity?.isChangingConfigurations != true) viewModel.stop() }
    }

    AzanSoundsScreen(
        settings = settings,
        playback = playback,
        alarmVolumeMuted = alarmMuted,
        onBack = onClose,
        onSoundSelected = viewModel::setSound,
        onTest = viewModel::test,
        onStop = viewModel::stop,
        onReset = viewModel::resetToDefaults,
        modifier = modifier,
    )
}

@Composable
fun AzanSoundsScreen(
    settings: AzanSoundSettings,
    playback: PlaybackState,
    alarmVolumeMuted: Boolean,
    onBack: () -> Unit,
    onSoundSelected: (Prayer, AzanSound) -> Unit,
    onTest: (Prayer) -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenContainer(title = stringResource(R.string.azan_sounds_title), onBack = onBack, modifier = modifier) {
        Text(stringResource(R.string.azan_sounds_intro), style = MaterialTheme.typography.bodyMedium)
        RecordingCard()
        Text(
            text = stringResource(if (alarmVolumeMuted) R.string.azan_alarm_volume_muted else R.string.azan_alarm_volume),
            style = MaterialTheme.typography.bodyMedium,
            color = if (alarmVolumeMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Prayer.entries.forEach { prayer ->
            PrayerSoundCard(
                prayer = prayer,
                sound = settings.soundFor(prayer),
                playback = playback,
                onSoundSelected = { onSoundSelected(prayer, it) },
                onTest = { onTest(prayer) },
                onStop = onStop,
            )
        }
        Text(
            text = stringResource(R.string.azan_sounds_fajr_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onReset) { Text(stringResource(R.string.azan_reset)) }
    }
}

@Composable
private fun RecordingCard() {
    val isTestTone = booleanResource(R.bool.bundled_audio_is_test_tone)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isTestTone) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.azan_sounds_recording), style = MaterialTheme.typography.labelLarge)
            if (isTestTone) {
                Text(stringResource(R.string.azan_sounds_test_tone_warning), style = MaterialTheme.typography.titleSmall)
            }
            Text(stringResource(R.string.bundled_audio_description), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PrayerSoundCard(
    prayer: Prayer,
    sound: AzanSound,
    playback: PlaybackState,
    onSoundSelected: (AzanSound) -> Unit,
    onTest: () -> Unit,
    onStop: () -> Unit,
) {
    val prayerName = stringResource(prayer.labelRes)
    val isPlayingThis = playback is PlaybackState.Playing && playback.prayer == prayer
    Card(modifier = Modifier.fillMaxWidth().testTag(prayerSoundTag(prayer))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = prayerName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (isPlayingThis) {
                    val stopDescription = stringResource(R.string.azan_test_stop_description, prayerName)
                    Button(onClick = onStop, modifier = Modifier.semantics { contentDescription = stopDescription }) {
                        Text(stringResource(R.string.azan_test_stop))
                    }
                } else {
                    val playDescription = stringResource(R.string.azan_test_play_description, prayerName)
                    OutlinedButton(
                        onClick = onTest,
                        enabled = sound != AzanSound.Silent,
                        modifier = Modifier.semantics { contentDescription = playDescription },
                    ) {
                        Text(stringResource(R.string.azan_test_play))
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AzanSound.entries.forEach { option ->
                    FilterChip(
                        selected = option == sound,
                        onClick = { onSoundSelected(option) },
                        label = { Text(stringResource(option.labelRes)) },
                        modifier = Modifier.testTag(soundChipTag(prayer, option)),
                    )
                }
            }
            when {
                isPlayingThis -> Text(stringResource(R.string.azan_playing), style = MaterialTheme.typography.bodySmall)
                sound == AzanSound.Silent -> Text(
                    stringResource(R.string.azan_silent_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                playback is PlaybackState.Failed && playback.prayer == prayer -> Text(
                    text = stringResource(
                        when (playback.error) {
                            PlaybackError.MissingAudio -> R.string.azan_error_missing
                            PlaybackError.PlaybackFailed -> R.string.azan_error_failed
                            PlaybackError.AudioFocusDenied -> R.string.azan_error_focus
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

val Prayer.labelRes: Int
    get() = when (this) {
        Prayer.Fajr -> R.string.prayer_fajr
        Prayer.Dhuhr -> R.string.prayer_dhuhr
        Prayer.Asr -> R.string.prayer_asr
        Prayer.Maghrib -> R.string.prayer_maghrib
        Prayer.Isha -> R.string.prayer_isha
    }

val AzanSound.labelRes: Int
    get() = when (this) {
        AzanSound.Full -> R.string.azan_sound_full
        AzanSound.Short -> R.string.azan_sound_short
        AzanSound.Silent -> R.string.azan_sound_silent
    }

fun prayerSoundTag(prayer: Prayer) = "sound_${prayer.name}"
fun soundChipTag(prayer: Prayer, sound: AzanSound) = "sound_${prayer.name}_${sound.name}"
