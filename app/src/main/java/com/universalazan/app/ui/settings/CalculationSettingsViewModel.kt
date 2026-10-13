package com.universalazan.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.universalazan.app.UniversalAzanApp
import com.universalazan.app.data.preferences.CalculationSettingsRepository
import com.universalazan.app.domain.prayer.AsrMadhab
import com.universalazan.app.domain.prayer.CalculationSettings
import com.universalazan.app.domain.prayer.HighLatitudeOption
import com.universalazan.app.domain.prayer.PrayerCalculationMethod
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalculationSettingsViewModel(
    private val repository: CalculationSettingsRepository,
) : ViewModel() {

    val settings: StateFlow<CalculationSettings> = repository.settings
        .catch { emit(CalculationSettings()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalculationSettings())

    fun setMethod(method: PrayerCalculationMethod) {
        viewModelScope.launch { repository.setMethod(method) }
    }

    fun setAsrMadhab(madhab: AsrMadhab) {
        viewModelScope.launch { repository.setAsrMadhab(madhab) }
    }

    fun setHighLatitude(option: HighLatitudeOption) {
        viewModelScope.launch { repository.setHighLatitude(option) }
    }

    fun resetToDefaults() {
        viewModelScope.launch { repository.resetToDefaults() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as UniversalAzanApp).container
                CalculationSettingsViewModel(container.calculationSettingsRepository)
            }
        }
    }
}
