package com.universalazan.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.universalazan.app.UniversalAzanApp
import com.universalazan.app.audio.AzanPlayer
import com.universalazan.app.audio.AzanSound
import com.universalazan.app.audio.AzanSoundSettings
import com.universalazan.app.audio.PlaybackState
import com.universalazan.app.data.preferences.AzanSoundRepository
import com.universalazan.app.domain.prayer.Prayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AzanSoundsViewModel(
    private val repository: AzanSoundRepository,
    private val player: AzanPlayer,
) : ViewModel() {

    val settings: StateFlow<AzanSoundSettings> = repository.settings
        .catch { emit(AzanSoundSettings()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AzanSoundSettings())

    val playback: StateFlow<PlaybackState> = player.state

    fun setSound(prayer: Prayer, sound: AzanSound) {
        // Changing to Silent (or another sound) while testing this prayer stops the old sound.
        val current = player.state.value
        if (current is PlaybackState.Playing && current.prayer == prayer) player.stop()
        viewModelScope.launch { repository.setSound(prayer, sound) }
    }

    fun test(prayer: Prayer) {
        player.play(prayer, settings.value.soundFor(prayer))
    }

    fun stop() = player.stop()

    fun isAlarmVolumeMuted(): Boolean = player.isAlarmVolumeMuted()

    fun resetToDefaults() {
        player.stop()
        viewModelScope.launch { repository.resetToDefaults() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as UniversalAzanApp).container
                AzanSoundsViewModel(container.azanSoundRepository, container.azanPlayer)
            }
        }
    }
}
