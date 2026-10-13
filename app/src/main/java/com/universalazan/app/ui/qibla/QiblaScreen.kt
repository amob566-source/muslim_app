package com.universalazan.app.ui.qibla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalazan.app.R
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.domain.qibla.QiblaResult
import com.universalazan.app.domain.qibla.compassPoint
import com.universalazan.app.sensors.CompassAccuracy
import com.universalazan.app.sensors.HeadingMath
import com.universalazan.app.ui.common.ScreenContainer
import com.universalazan.app.ui.theme.UniversalAzanTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun QiblaRoute(
    onChooseLocation: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QiblaViewModel = viewModel(factory = QiblaViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val compass by viewModel.compass.collectAsStateWithLifecycle()
    QiblaScreen(state = state, compass = compass, onChooseLocation = onChooseLocation, modifier = modifier)
}

/** The calculated Qibla bearing and, where the phone has the sensors, a live compass. */
@Composable
fun QiblaScreen(
    state: QiblaUiState,
    compass: CompassUiState,
    onChooseLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenContainer(title = stringResource(R.string.nav_qibla), modifier = modifier) {
        when (state) {
            QiblaUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            QiblaUiState.NoLocation -> NoLocationCard(onChooseLocation)
            is QiblaUiState.Ready -> {
                ResultCard(state, onChooseLocation)
                val result = state.result
                if (result is QiblaResult.Direction) {
                    CompassSection(compass, result.bearingDegrees)
                }
            }
        }
    }
}

@Composable
private fun CompassSection(compass: CompassUiState, qiblaBearing: Double) {
    val locale = LocalConfiguration.current.locales[0]
    Card(modifier = Modifier.fillMaxWidth().testTag(COMPASS_SECTION_TAG)) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.compass_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().semantics { heading() },
            )
            when (compass) {
                CompassUiState.Off -> Unit
                CompassUiState.Waiting -> {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.compass_waiting), style = MaterialTheme.typography.bodyMedium)
                }
                CompassUiState.Stopped -> Text(
                    stringResource(R.string.compass_stopped),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                CompassUiState.Unavailable -> {
                    Text(stringResource(R.string.compass_unavailable_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.compass_unavailable), style = MaterialTheme.typography.bodyMedium)
                }
                is CompassUiState.Live -> LiveCompass(compass, qiblaBearing, locale)
            }
        }
    }
    if (compass is CompassUiState.Unavailable) {
        Text(stringResource(R.string.qibla_true_north_note), style = MaterialTheme.typography.bodyMedium)
    }
    Text(
        text = stringResource(R.string.compass_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LiveCompass(compass: CompassUiState.Live, qiblaBearing: Double, locale: Locale) {
    val turn = HeadingMath.signedTurn(compass.trueHeading, qiblaBearing)
    val facing = HeadingMath.isFacing(compass.trueHeading, qiblaBearing)
    val instruction = when {
        facing -> stringResource(R.string.compass_facing)
        turn > 0 -> stringResource(R.string.compass_turn_right, Math.round(turn).toString())
        else -> stringResource(R.string.compass_turn_left, Math.round(-turn).toString())
    }
    QiblaCompassDial(
        heading = compass.trueHeading,
        qiblaBearing = qiblaBearing,
        facing = facing,
        description = stringResource(R.string.compass_dial_description, instruction),
    )
    Text(
        text = instruction,
        style = MaterialTheme.typography.headlineSmall,
        color = if (facing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag(COMPASS_INSTRUCTION_TAG),
    )
    val headingRounded = Math.round(compass.trueHeading).toInt() % 360
    Text(
        text = stringResource(R.string.compass_heading, headingRounded.toString(), compassPoint(compass.trueHeading)),
        style = MaterialTheme.typography.bodyMedium,
    )
    val (accuracyRes, isProblem) = when (compass.accuracy) {
        CompassAccuracy.High -> R.string.compass_accuracy_high to false
        CompassAccuracy.Medium -> R.string.compass_accuracy_medium to false
        CompassAccuracy.Low -> R.string.compass_accuracy_low to true
        CompassAccuracy.Unreliable -> R.string.compass_accuracy_unreliable to true
        CompassAccuracy.Unknown -> R.string.compass_accuracy_unknown to false
    }
    Text(
        text = stringResource(accuracyRes),
        style = MaterialTheme.typography.bodyMedium,
        color = if (isProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
    if (compass.interference) {
        Text(
            text = stringResource(R.string.compass_interference),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Text(
        text = stringResource(R.string.compass_how_to),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = stringResource(R.string.compass_true_north, String.format(locale, "%+.1f", compass.declination)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NoLocationCard(onChooseLocation: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.qibla_direction_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.qibla_no_location), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.qibla_no_location_hint), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onChooseLocation) { Text(stringResource(R.string.choose_location)) }
        }
    }
}

@Composable
private fun ResultCard(state: QiblaUiState.Ready, onChooseLocation: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.qibla_direction_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.qibla_for_location, state.location.name),
                style = MaterialTheme.typography.titleMedium,
            )
            when (val result = state.result) {
                is QiblaResult.Direction -> {
                    val degrees = formatDegrees(result.bearingDegrees, locale)
                    val point = compassPoint(result.bearingDegrees)
                    val spoken = stringResource(R.string.qibla_bearing_spoken, degrees, point)
                    Text(
                        text = stringResource(R.string.qibla_bearing, degrees),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .testTag(QIBLA_BEARING_TAG)
                            .clearAndSetSemantics { contentDescription = spoken },
                    )
                    Text(
                        text = stringResource(R.string.qibla_from_true_north, point),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(
                            R.string.qibla_distance,
                            NumberFormat.getIntegerInstance(locale).format(Math.round(result.distanceKm)),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                QiblaResult.AtKaaba -> Text(stringResource(R.string.qibla_at_kaaba), style = MaterialTheme.typography.bodyLarge)
                QiblaResult.Antipode -> Text(stringResource(R.string.qibla_antipode), style = MaterialTheme.typography.bodyLarge)
                QiblaResult.InvalidCoordinates -> Text(
                    stringResource(R.string.qibla_invalid_coordinates),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(onClick = onChooseLocation) { Text(stringResource(R.string.change_location)) }
        }
    }
}

/** One decimal place, e.g. "119.0", using the phone's number style. 359.96 shows as "0.0", not "360.0". */
private fun formatDegrees(degrees: Double, locale: Locale): String {
    val rounded = Math.round(degrees * 10) / 10.0
    return String.format(locale, "%.1f", if (rounded >= 360.0) 0.0 else rounded)
}

const val QIBLA_BEARING_TAG = "qibla_bearing"
const val COMPASS_SECTION_TAG = "compass_section"
const val COMPASS_INSTRUCTION_TAG = "compass_instruction"

@Preview(showBackground = true)
@Composable
private fun QiblaScreenPreview() {
    UniversalAzanTheme {
        QiblaScreen(
            state = QiblaViewModel.stateFor(
                SavedLocation("London", 51.5085, -0.1257, "Europe/London", LocationSource.City),
            ),
            compass = CompassUiState.Live(trueHeading = 90.0, accuracy = CompassAccuracy.High, interference = false, declination = 0.5),
            onChooseLocation = {},
        )
    }
}
