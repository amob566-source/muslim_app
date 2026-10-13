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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalazan.app.R
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.domain.qibla.QiblaResult
import com.universalazan.app.domain.qibla.compassPoint
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
    QiblaScreen(state = state, onChooseLocation = onChooseLocation, modifier = modifier)
}

/** Stage 4: the calculated bearing in degrees. The live compass comes in Stage 5. */
@Composable
fun QiblaScreen(
    state: QiblaUiState,
    onChooseLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenContainer(title = stringResource(R.string.nav_qibla), modifier = modifier) {
        when (state) {
            QiblaUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            QiblaUiState.NoLocation -> NoLocationCard(onChooseLocation)
            is QiblaUiState.Ready -> {
                ResultCard(state, onChooseLocation)
                if (state.result is QiblaResult.Direction) {
                    Text(
                        text = stringResource(R.string.qibla_true_north_note),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.qibla_compass_coming),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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

@Preview(showBackground = true)
@Composable
private fun QiblaScreenPreview() {
    UniversalAzanTheme {
        QiblaScreen(
            state = QiblaViewModel.stateFor(
                SavedLocation("London", 51.5085, -0.1257, "Europe/London", LocationSource.City),
            ),
            onChooseLocation = {},
        )
    }
}
