package com.universalazan.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.universalazan.app.UniversalAzanApp
import com.universalazan.app.audio.AzanSoundSettings
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.data.preferences.AzanSoundRepository
import com.universalazan.app.data.preferences.CalculationSettingsRepository
import com.universalazan.app.data.preferences.LocationRepository
import com.universalazan.app.domain.prayer.CalculationSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    locationRepository: LocationRepository,
    calculationSettingsRepository: CalculationSettingsRepository,
    azanSoundRepository: AzanSoundRepository,
) : ViewModel() {

    val currentLocation: StateFlow<SavedLocation?> = locationRepository.savedLocation
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val calculationSettings: StateFlow<CalculationSettings> = calculationSettingsRepository.settings
        .catch { emit(CalculationSettings()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalculationSettings())

    val azanSounds: StateFlow<AzanSoundSettings> = azanSoundRepository.settings
        .catch { emit(AzanSoundSettings()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AzanSoundSettings())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as UniversalAzanApp).container
                SettingsViewModel(
                    container.locationRepository,
                    container.calculationSettingsRepository,
                    container.azanSoundRepository,
                )
            }
        }
    }
}
