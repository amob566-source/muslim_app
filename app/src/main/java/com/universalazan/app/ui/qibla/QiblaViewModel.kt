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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface QiblaUiState {
    data object Loading : QiblaUiState
    data object NoLocation : QiblaUiState
    data class Ready(val location: SavedLocation, val result: QiblaResult) : QiblaUiState
}

/** Recalculates the Qibla bearing whenever the saved location changes. Uses no GPS or internet. */
class QiblaViewModel(locationRepository: LocationRepository) : ViewModel() {

    val uiState: StateFlow<QiblaUiState> = locationRepository.savedLocation
        .map { location -> stateFor(location) }
        .catch { emit(QiblaUiState.NoLocation) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QiblaUiState.Loading)

    companion object {
        fun stateFor(location: SavedLocation?): QiblaUiState =
            if (location == null) {
                QiblaUiState.NoLocation
            } else {
                QiblaUiState.Ready(location, QiblaCalculator.calculate(location.latitude, location.longitude))
            }

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as UniversalAzanApp).container
                QiblaViewModel(container.locationRepository)
            }
        }
    }
}
