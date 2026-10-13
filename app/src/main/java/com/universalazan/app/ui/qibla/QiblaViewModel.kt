package com.universalazan.app.ui.qibla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.universalazan.app.UniversalAzanApp
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.data.preferences.LocationRepository
import com.universalazan.app.domain.qibla.QiblaCalculator
import com.universalazan.app.domain.qibla.QiblaResult
import com.universalazan.app.domain.qibla.normalizeDegrees
import com.universalazan.app.sensors.CompassAccuracy
import com.universalazan.app.sensors.CompassSample
import com.universalazan.app.sensors.CompassSensor
import com.universalazan.app.sensors.CompassSensorMode
import com.universalazan.app.sensors.HeadingMath
import com.universalazan.app.sensors.magneticDeclination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

sealed interface QiblaUiState {
    data object Loading : QiblaUiState
    data object NoLocation : QiblaUiState
    data class Ready(val location: SavedLocation, val result: QiblaResult) : QiblaUiState
}

sealed interface CompassUiState {
    /** No compass is needed (no location, or no single Qibla direction). Sensors are off. */
    data object Off : CompassUiState

    /** This device has no compass sensor. */
    data object Unavailable : CompassUiState

    /** Sensors are on, waiting for the first reading. */
    data object Waiting : CompassUiState

    /** The sensors stopped sending readings. */
    data object Stopped : CompassUiState

    data class Live(
        /** Where the top of the phone points, degrees clockwise from TRUE north. */
        val trueHeading: Double,
        val accuracy: CompassAccuracy,
        /** The measured magnetic field is outside Earth's normal range. */
        val interference: Boolean,
        /** Correction applied from magnetic to true north (east positive). */
        val declination: Double,
    ) : CompassUiState
}

/**
 * Recalculates the Qibla bearing whenever the saved location changes (no GPS or internet),
 * and reads the compass sensors only while the Qibla screen is visible.
 */
class QiblaViewModel(
    locationRepository: LocationRepository,
    private val compassSensor: CompassSensor,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val uiState: StateFlow<QiblaUiState> = locationRepository.savedLocation
        .map { location -> stateFor(location) }
        .catch { emit(QiblaUiState.NoLocation) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QiblaUiState.Loading)

    /** Stops the sensors one second after the screen is hidden (rotation does not restart them). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val compass: StateFlow<CompassUiState> = uiState
        .map { (it as? QiblaUiState.Ready)?.takeIf { ready -> ready.result is QiblaResult.Direction }?.location }
        .flatMapLatest { location ->
            when {
                location == null -> flowOf(CompassUiState.Off)
                compassSensor.mode == CompassSensorMode.None -> flowOf(CompassUiState.Unavailable)
                else -> {
                    // Declination changes slowly (fraction of a degree per year), so once per location is enough.
                    val declination = magneticDeclination(location.latitude, location.longitude, clock())
                    compassSensor.samples()
                        .map<CompassSample, CompassUiState> { liveState(it, declination) }
                        .onStart { emit(CompassUiState.Waiting) }
                        .onCompletion { cause -> if (cause == null) emit(CompassUiState.Stopped) }
                }
            }
        }
        .catch { emit(CompassUiState.Stopped) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1_000), CompassUiState.Off)

    companion object {
        fun stateFor(location: SavedLocation?): QiblaUiState =
            if (location == null) {
                QiblaUiState.NoLocation
            } else {
                QiblaUiState.Ready(location, QiblaCalculator.calculate(location.latitude, location.longitude))
            }

        /** Pure function so it can be tested without sensors. */
        fun liveState(sample: CompassSample, declination: Double): CompassUiState.Live =
            CompassUiState.Live(
                // Rounded to 0.5°: finer than the screen shows or a phone compass can measure, and
                // it stops the dial being redrawn ~15 times a second for invisible sensor noise.
                trueHeading = normalizeDegrees(Math.round((sample.magneticHeading + declination) * 2) / 2.0),
                accuracy = sample.accuracy,
                interference = sample.fieldStrengthMicroTesla?.let(HeadingMath::isMagneticInterference) ?: false,
                declination = declination,
            )

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as UniversalAzanApp).container
                QiblaViewModel(container.locationRepository, container.compassSensor)
            }
        }
    }
}
