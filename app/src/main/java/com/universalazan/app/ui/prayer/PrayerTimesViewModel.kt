package com.universalazan.app.ui.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.universalazan.app.R
import com.universalazan.app.UniversalAzanApp
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.data.preferences.CalculationSettingsRepository
import com.universalazan.app.data.preferences.LocationRepository
import com.universalazan.app.domain.prayer.CalculationSettings
import com.universalazan.app.domain.prayer.PrayerCalculationResult
import com.universalazan.app.domain.prayer.PrayerTimeCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PrayerTimesViewModel(
    locationRepository: LocationRepository,
    settingsRepository: CalculationSettingsRepository,
    private val clock: () -> Instant = Instant::now,
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    /**
     * Ticks once a minute while the screen is visible, so the date rolls over at midnight
     * and the "next prayer" marker moves on. Times are only recalculated, never fetched.
     */
    private val minuteTicks: Flow<Instant> = flow {
        while (true) {
            val now = clock()
            emit(now)
            delay(MILLIS_PER_MINUTE - now.toEpochMilli() % MILLIS_PER_MINUTE + 50)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PrayerTimesUiState> = retryTrigger
        .flatMapLatest {
            combine<_, _, _, PrayerTimesUiState>(
                locationRepository.savedLocation,
                settingsRepository.settings,
                minuteTicks,
            ) { location, settings, now -> buildState(location, settings, now) }
                .distinctUntilChanged()
                .onStart { emit(PrayerTimesUiState.Loading) }
                .catch { emit(PrayerTimesUiState.Error(R.string.error_prayer_times)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrayerTimesUiState.Loading)

    fun retry() {
        retryTrigger.value++
    }

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as UniversalAzanApp).container
                PrayerTimesViewModel(container.locationRepository, container.calculationSettingsRepository)
            }
        }

        /** Pure function so it can be tested without Android. */
        fun buildState(
            location: SavedLocation?,
            settings: CalculationSettings,
            now: Instant,
        ): PrayerTimesUiState.Ready {
            if (location == null) {
                return PrayerTimesUiState.Ready(
                    location = null,
                    date = now.atZone(ZoneId.systemDefault()).toLocalDate(),
                    prayers = notCalculatedPrayerRows(),
                    settings = settings,
                    problem = PrayerProblem.NoLocation,
                )
            }
            val zone = location.zoneId
                ?: return PrayerTimesUiState.Ready(
                    location = location,
                    date = now.atZone(ZoneId.systemDefault()).toLocalDate(),
                    prayers = notCalculatedPrayerRows(),
                    settings = settings,
                    problem = PrayerProblem.InvalidTimeZone,
                )
            // "Today" is the date at the selected location, not necessarily the phone's date.
            val nowThere = now.atZone(zone)
            val date = nowThere.toLocalDate()
            return when (val result = PrayerTimeCalculator.calculate(location.latitude, location.longitude, date, zone, settings)) {
                is PrayerCalculationResult.Success -> PrayerTimesUiState.Ready(
                    location = location,
                    date = date,
                    prayers = calculatedPrayerRows(result.times, nowThere),
                    settings = settings,
                )
                PrayerCalculationResult.InvalidCoordinates -> PrayerTimesUiState.Ready(
                    location, date, notCalculatedPrayerRows(), settings, PrayerProblem.InvalidCoordinates,
                )
                PrayerCalculationResult.SunDoesNotRiseOrSet -> PrayerTimesUiState.Ready(
                    location, date, notCalculatedPrayerRows(), settings, PrayerProblem.SunDoesNotRiseOrSet,
                )
            }
        }
    }
}
