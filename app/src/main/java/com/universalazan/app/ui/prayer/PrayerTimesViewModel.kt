package com.universalazan.app.ui.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.universalazan.app.R
import com.universalazan.app.UniversalAzanApp
import com.universalazan.app.data.preferences.LocationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

class PrayerTimesViewModel(locationRepository: LocationRepository) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PrayerTimesUiState> = retryTrigger
        .flatMapLatest {
            locationRepository.savedLocation
                .map<_, PrayerTimesUiState> { location ->
                    // "Today" is the date at the selected location, not necessarily the phone's.
                    val zone = location?.zoneId ?: ZoneId.systemDefault()
                    PrayerTimesUiState.Ready(
                        location = location,
                        date = LocalDate.now(zone),
                        prayers = notCalculatedPrayerRows(),
                    )
                }
                .onStart { emit(PrayerTimesUiState.Loading) }
                .catch { emit(PrayerTimesUiState.Error(R.string.error_prayer_times)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrayerTimesUiState.Loading)

    fun retry() {
        retryTrigger.value++
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as UniversalAzanApp).container
                PrayerTimesViewModel(container.locationRepository)
            }
        }
    }
}
