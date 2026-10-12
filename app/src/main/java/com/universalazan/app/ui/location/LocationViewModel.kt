package com.universalazan.app.ui.location

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.universalazan.app.R
import com.universalazan.app.UniversalAzanApp
import com.universalazan.app.data.locations.City
import com.universalazan.app.data.locations.CityRepository
import com.universalazan.app.data.locations.CitySearch
import com.universalazan.app.data.locations.DeviceLocationProvider
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.data.preferences.LocationRepository
import com.universalazan.app.domain.location.CoordinateAxis
import com.universalazan.app.domain.location.CoordinateParseResult
import com.universalazan.app.domain.location.formatCoordinates
import com.universalazan.app.domain.location.parseCoordinate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

sealed interface DeviceLocationState {
    data object Idle : DeviceLocationState
    data object Searching : DeviceLocationState
    data class PermissionDenied(val permanently: Boolean) : DeviceLocationState
    data object LocationOff : DeviceLocationState
    data object NoFix : DeviceLocationState
}

data class LocationUiState(
    val current: SavedLocation? = null,
    val cityQuery: String = "",
    val cityResults: List<City> = emptyList(),
    val citiesLoading: Boolean = true,
    val citiesFailed: Boolean = false,
    val manualName: String = "",
    val manualLatitude: String = "",
    val manualLongitude: String = "",
    val manualTimeZoneId: String = ZoneId.systemDefault().id,
    @StringRes val manualLatitudeError: Int? = null,
    @StringRes val manualLongitudeError: Int? = null,
    val device: DeviceLocationState = DeviceLocationState.Idle,
)

class LocationViewModel(
    private val locationRepository: LocationRepository,
    private val cityRepository: CityRepository,
    private val deviceLocationProvider: DeviceLocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(LocationUiState())
    val state: StateFlow<LocationUiState> = _state.asStateFlow()

    private val savedChannel = Channel<Unit>(Channel.BUFFERED)

    /** Emits once each time a location has been saved, so the screen can close. */
    val saved: Flow<Unit> = savedChannel.receiveAsFlow()

    private var citySearch: CitySearch? = null
    private var searchJob: Job? = null
    private var deviceJob: Job? = null

    init {
        viewModelScope.launch {
            locationRepository.savedLocation
                .catch { emit(null) }
                .collect { location -> _state.update { it.copy(current = location) } }
        }
        viewModelScope.launch {
            try {
                citySearch = cityRepository.citySearch()
                _state.update { it.copy(citiesLoading = false) }
                runCitySearch(_state.value.cityQuery)
            } catch (e: Exception) {
                _state.update { it.copy(citiesLoading = false, citiesFailed = true) }
            }
        }
    }

    // --- City search ---

    fun onCityQueryChange(query: String) {
        _state.update { it.copy(cityQuery = query) }
        runCitySearch(query)
    }

    private fun runCitySearch(query: String) {
        val search = citySearch ?: return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val results = withContext(Dispatchers.Default) { search.search(query) }
            _state.update { it.copy(cityResults = results) }
        }
    }

    fun selectCity(city: City) = save(city.toSavedLocation())

    // --- Manual coordinates ---

    fun onManualNameChange(value: String) = _state.update { it.copy(manualName = value) }

    fun onManualLatitudeChange(value: String) =
        _state.update { it.copy(manualLatitude = value, manualLatitudeError = null) }

    fun onManualLongitudeChange(value: String) =
        _state.update { it.copy(manualLongitude = value, manualLongitudeError = null) }

    fun onManualTimeZoneChosen(timeZoneId: String) =
        _state.update { it.copy(manualTimeZoneId = timeZoneId) }

    fun saveManual() {
        val s = _state.value
        val latitude = parseCoordinate(s.manualLatitude, CoordinateAxis.Latitude)
        val longitude = parseCoordinate(s.manualLongitude, CoordinateAxis.Longitude)
        _state.update {
            it.copy(
                manualLatitudeError = errorFor(latitude, CoordinateAxis.Latitude),
                manualLongitudeError = errorFor(longitude, CoordinateAxis.Longitude),
            )
        }
        if (latitude !is CoordinateParseResult.Valid || longitude !is CoordinateParseResult.Valid) return
        val name = s.manualName.trim().ifEmpty { formatCoordinates(latitude.value, longitude.value) }
        save(SavedLocation(name, latitude.value, longitude.value, s.manualTimeZoneId, LocationSource.Manual))
    }

    // --- Device location ---

    fun hasLocationPermission(): Boolean = deviceLocationProvider.hasPermission()

    /** Call after permission has been granted. */
    fun useDeviceLocation(locationName: String) {
        deviceJob?.cancel()
        _state.update { it.copy(device = DeviceLocationState.Searching) }
        deviceJob = viewModelScope.launch {
            when (val result = deviceLocationProvider.currentLocation()) {
                is DeviceLocationProvider.Result.Success -> {
                    _state.update { it.copy(device = DeviceLocationState.Idle) }
                    // The phone is at this location, so its time zone is the best available
                    // guess. The screen tells the user and lets them change it.
                    save(
                        SavedLocation(
                            name = locationName,
                            latitude = result.latitude,
                            longitude = result.longitude,
                            timeZoneId = ZoneId.systemDefault().id,
                            source = LocationSource.Device,
                        ),
                    )
                }
                DeviceLocationProvider.Result.NoPermission ->
                    _state.update { it.copy(device = DeviceLocationState.PermissionDenied(false)) }
                DeviceLocationProvider.Result.LocationOff ->
                    _state.update { it.copy(device = DeviceLocationState.LocationOff) }
                DeviceLocationProvider.Result.NoFix ->
                    _state.update { it.copy(device = DeviceLocationState.NoFix) }
            }
        }
    }

    fun onPermissionDenied(permanently: Boolean) =
        _state.update { it.copy(device = DeviceLocationState.PermissionDenied(permanently)) }

    fun cancelDeviceLocation() {
        deviceJob?.cancel()
        _state.update { it.copy(device = DeviceLocationState.Idle) }
    }

    // --- Saved location ---

    fun changeCurrentTimeZone(timeZoneId: String) {
        viewModelScope.launch { locationRepository.updateTimeZone(timeZoneId) }
    }

    private fun save(location: SavedLocation) {
        viewModelScope.launch {
            locationRepository.save(location)
            savedChannel.send(Unit)
        }
    }

    private fun errorFor(result: CoordinateParseResult, axis: CoordinateAxis): Int? = when (result) {
        is CoordinateParseResult.Valid -> null
        CoordinateParseResult.Empty -> R.string.error_coordinate_empty
        CoordinateParseResult.NotANumber -> R.string.error_coordinate_not_number
        CoordinateParseResult.OutOfRange -> when (axis) {
            CoordinateAxis.Latitude -> R.string.error_latitude_range
            CoordinateAxis.Longitude -> R.string.error_longitude_range
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as UniversalAzanApp).container
                LocationViewModel(
                    container.locationRepository,
                    container.cityRepository,
                    container.deviceLocationProvider,
                )
            }
        }
    }
}
