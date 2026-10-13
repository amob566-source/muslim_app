package com.universalazan.app.ui.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalazan.app.R
import com.universalazan.app.data.locations.City
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.domain.location.formatCoordinates
import com.universalazan.app.ui.common.ScreenContainer
import java.time.ZoneId

private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private enum class TimeZoneTarget { None, Manual, Current }

/** Location screen with its ViewModel, permission request and time zone picker. */
@Composable
fun LocationRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = viewModel(factory = LocationViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var timeZoneTarget by rememberSaveable { mutableStateOf(TimeZoneTarget.None) }
    val activity = LocalActivity.current
    val context = LocalContext.current
    val deviceLocationName = stringResource(R.string.device_location_name)

    BackHandler(onBack = onClose)
    LaunchedEffect(viewModel) { viewModel.saved.collect { onClose() } }
    // Leaving the screen stops a running GPS search, but rotating the screen does not.
    DisposableEffect(viewModel) {
        onDispose {
            if (activity?.isChangingConfigurations != true) viewModel.cancelDeviceLocation()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            viewModel.useDeviceLocation(deviceLocationName)
        } else {
            // If Android will no longer show the request, the user must use system settings.
            val canAskAgain = activity != null && LocationPermissions.any {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
            viewModel.onPermissionDenied(permanently = !canAskAgain)
        }
    }

    when (timeZoneTarget) {
        TimeZoneTarget.None -> LocationScreen(
            state = state,
            onBack = onClose,
            onCityQueryChange = viewModel::onCityQueryChange,
            onCitySelected = viewModel::selectCity,
            onUseDeviceLocation = {
                if (viewModel.hasLocationPermission()) {
                    viewModel.useDeviceLocation(deviceLocationName)
                } else {
                    permissionLauncher.launch(LocationPermissions)
                }
            },
            onCancelDeviceLocation = viewModel::cancelDeviceLocation,
            onOpenAppSettings = { openAppSettings(context) },
            onOpenLocationSettings = { openLocationSettings(context) },
            onManualNameChange = viewModel::onManualNameChange,
            onManualLatitudeChange = viewModel::onManualLatitudeChange,
            onManualLongitudeChange = viewModel::onManualLongitudeChange,
            onPickManualTimeZone = { timeZoneTarget = TimeZoneTarget.Manual },
            onSaveManual = viewModel::saveManual,
            onChangeCurrentTimeZone = { timeZoneTarget = TimeZoneTarget.Current },
            modifier = modifier,
        )
        TimeZoneTarget.Manual -> TimeZonePicker(
            selectedId = state.manualTimeZoneId,
            onPick = {
                viewModel.onManualTimeZoneChosen(it)
                timeZoneTarget = TimeZoneTarget.None
            },
            onBack = { timeZoneTarget = TimeZoneTarget.None },
            modifier = modifier,
        )
        TimeZoneTarget.Current -> TimeZonePicker(
            selectedId = state.current?.timeZoneId,
            onPick = {
                viewModel.changeCurrentTimeZone(it)
                timeZoneTarget = TimeZoneTarget.None
            },
            onBack = { timeZoneTarget = TimeZoneTarget.None },
            modifier = modifier,
        )
    }
}

@Composable
fun LocationScreen(
    state: LocationUiState,
    onBack: () -> Unit,
    onCityQueryChange: (String) -> Unit,
    onCitySelected: (City) -> Unit,
    onUseDeviceLocation: () -> Unit,
    onCancelDeviceLocation: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onManualNameChange: (String) -> Unit,
    onManualLatitudeChange: (String) -> Unit,
    onManualLongitudeChange: (String) -> Unit,
    onPickManualTimeZone: () -> Unit,
    onSaveManual: () -> Unit,
    onChangeCurrentTimeZone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenContainer(
        title = stringResource(R.string.location_screen_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        CurrentSelectionCard(state.current, onChangeCurrentTimeZone)
        CitySearchSection(state, onCityQueryChange, onCitySelected)
        DeviceLocationSection(
            device = state.device,
            onUseDeviceLocation = onUseDeviceLocation,
            onCancel = onCancelDeviceLocation,
            onOpenAppSettings = onOpenAppSettings,
            onOpenLocationSettings = onOpenLocationSettings,
        )
        ManualSection(
            state = state,
            onNameChange = onManualNameChange,
            onLatitudeChange = onManualLatitudeChange,
            onLongitudeChange = onManualLongitudeChange,
            onPickTimeZone = onPickManualTimeZone,
            onSave = onSaveManual,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun CurrentSelectionCard(current: SavedLocation?, onChangeTimeZone: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.location_current_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (current == null) {
                Text(stringResource(R.string.location_not_selected), style = MaterialTheme.typography.titleLarge)
                return@Column
            }
            Text(current.name, style = MaterialTheme.typography.titleLarge)
            Text(formatCoordinates(current.latitude, current.longitude), style = MaterialTheme.typography.bodyMedium)
            if (current.zoneId != null) {
                Text(
                    stringResource(R.string.location_time_zone, current.timeZoneId),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    stringResource(R.string.location_time_zone_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = stringResource(
                    when (current.source) {
                        LocationSource.City -> R.string.location_source_city
                        LocationSource.Manual -> R.string.location_source_manual
                        LocationSource.Device -> R.string.location_source_device
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onChangeTimeZone) { Text(stringResource(R.string.change_time_zone)) }
        }
    }
}

@Composable
private fun CitySearchSection(
    state: LocationUiState,
    onQueryChange: (String) -> Unit,
    onCitySelected: (City) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.city_search_title))
        OutlinedTextField(
            value = state.cityQuery,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.city_search_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CITY_SEARCH_TAG),
        )
        when {
            state.citiesLoading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.city_search_loading), style = MaterialTheme.typography.bodyMedium)
            }
            state.citiesFailed -> Text(
                stringResource(R.string.city_search_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            state.cityQuery.isNotBlank() && state.cityResults.isEmpty() -> Text(
                stringResource(R.string.city_search_no_results),
                style = MaterialTheme.typography.bodyMedium,
            )
            state.cityResults.isNotEmpty() -> Card(modifier = Modifier.fillMaxWidth()) {
                state.cityResults.forEachIndexed { index, city ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    CityResultItem(city, onClick = { onCitySelected(city) })
                }
            }
        }
        Text(
            text = stringResource(R.string.city_search_attribution),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CityResultItem(city: City, onClick: () -> Unit) {
    val place = listOf(city.region, city.countryName()).filter { it.isNotBlank() }.joinToString(", ")
    ListItem(
        headlineContent = { Text(city.name) },
        supportingContent = { Text("$place · ${city.timeZoneId}") },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun DeviceLocationSection(
    device: DeviceLocationState,
    onUseDeviceLocation: () -> Unit,
    onCancel: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.device_location_title))
        Text(stringResource(R.string.device_location_explanation), style = MaterialTheme.typography.bodyMedium)
        if (device == DeviceLocationState.Searching) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                Text(
                    stringResource(R.string.device_location_searching),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            return@Column
        }
        Button(onClick = onUseDeviceLocation) { Text(stringResource(R.string.device_location_button)) }
        val message = when (device) {
            is DeviceLocationState.PermissionDenied -> if (device.permanently) {
                R.string.device_location_denied_permanently
            } else {
                R.string.device_location_denied
            }
            DeviceLocationState.LocationOff -> R.string.device_location_off
            DeviceLocationState.NoFix -> R.string.device_location_no_fix
            else -> null
        }
        if (message != null) {
            Text(
                stringResource(message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (device is DeviceLocationState.PermissionDenied && device.permanently) {
            OutlinedButton(onClick = onOpenAppSettings) { Text(stringResource(R.string.open_app_settings)) }
        }
        if (device == DeviceLocationState.LocationOff) {
            OutlinedButton(onClick = onOpenLocationSettings) {
                Text(stringResource(R.string.open_location_settings))
            }
        }
    }
}

@Composable
private fun ManualSection(
    state: LocationUiState,
    onNameChange: (String) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onPickTimeZone: () -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.manual_title))
        Text(stringResource(R.string.manual_hint), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = state.manualName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.manual_name_label)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MANUAL_NAME_TAG),
        )
        CoordinateField(
            value = state.manualLatitude,
            onValueChange = onLatitudeChange,
            label = stringResource(R.string.manual_latitude_label),
            errorRes = state.manualLatitudeError,
            testTag = MANUAL_LATITUDE_TAG,
        )
        CoordinateField(
            value = state.manualLongitude,
            onValueChange = onLongitudeChange,
            label = stringResource(R.string.manual_longitude_label),
            errorRes = state.manualLongitudeError,
            testTag = MANUAL_LONGITUDE_TAG,
        )
        Text(
            text = stringResource(R.string.manual_time_zone_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val zoneText = if (state.manualTimeZoneId == ZoneId.systemDefault().id) {
            stringResource(R.string.manual_time_zone_phone, state.manualTimeZoneId)
        } else {
            state.manualTimeZoneId
        }
        Text(zoneText, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag(MANUAL_TIME_ZONE_TAG))
        Text(
            stringResource(R.string.manual_time_zone_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onPickTimeZone) { Text(stringResource(R.string.manual_choose_time_zone)) }
        Button(onClick = onSave, modifier = Modifier.testTag(MANUAL_SAVE_TAG)) {
            Text(stringResource(R.string.manual_save))
        }
    }
}

@Composable
private fun CoordinateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorRes: Int?,
    testTag: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = errorRes != null,
        supportingText = errorRes?.let { { Text(stringResource(it)) } },
        // A plain text keyboard: number pads on some phones have no minus key.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    )
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    runCatching { context.startActivity(intent) }
}

private fun openLocationSettings(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
}

const val CITY_SEARCH_TAG = "city_search"
const val MANUAL_NAME_TAG = "manual_name"
const val MANUAL_LATITUDE_TAG = "manual_latitude"
const val MANUAL_LONGITUDE_TAG = "manual_longitude"
const val MANUAL_TIME_ZONE_TAG = "manual_time_zone"
const val MANUAL_SAVE_TAG = "manual_save"
