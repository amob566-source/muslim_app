package com.universalazan.app.ui.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalazan.app.R
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.domain.location.formatCoordinates
import com.universalazan.app.ui.common.ScreenContainer
import com.universalazan.app.ui.theme.UniversalAzanTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Entry point used by navigation. */
@Composable
fun PrayerTimesRoute(
    onChooseLocation: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrayerTimesViewModel = viewModel(factory = PrayerTimesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PrayerTimesScreen(
        state = state,
        onChooseLocation = onChooseLocation,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun PrayerTimesScreen(
    state: PrayerTimesUiState,
    onChooseLocation: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenContainer(title = stringResource(R.string.nav_prayer_times), modifier = modifier) {
        when (state) {
            PrayerTimesUiState.Loading -> LoadingContent()
            is PrayerTimesUiState.Error -> ErrorContent(state.messageRes, onRetry)
            is PrayerTimesUiState.Ready -> ReadyContent(state, onChooseLocation)
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.loading_prayer_times),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ErrorContent(messageRes: Int, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.error_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(messageRes), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun ReadyContent(state: PrayerTimesUiState.Ready, onChooseLocation: () -> Unit) {
    LocationCard(state.location, onChooseLocation)
    DateLine(state.date)
    if (state.prayers.any { it.time == null }) {
        NotCalculatedNotice()
    }
    PrayerList(state.prayers)
}

@Composable
private fun LocationCard(location: SavedLocation?, onChooseLocation: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.location_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = location?.name ?: stringResource(R.string.location_not_selected),
                style = MaterialTheme.typography.titleLarge,
            )
            if (location == null) {
                Text(
                    text = stringResource(R.string.location_not_selected_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = formatCoordinates(location.latitude, location.longitude),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (location.zoneId != null) {
                    Text(
                        text = stringResource(R.string.location_time_zone, location.timeZoneId),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.location_time_zone_invalid),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            OutlinedButton(onClick = onChooseLocation) {
                Text(
                    stringResource(
                        if (location == null) R.string.choose_location else R.string.change_location,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DateLine(date: LocalDate) {
    val formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(date)
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = stringResource(R.string.today_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = formatted, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun NotCalculatedNotice() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Text(
            text = stringResource(R.string.not_calculated_notice),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun PrayerList(prayers: List<PrayerRowUi>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            prayers.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                PrayerRowItem(row)
            }
        }
    }
}

@Composable
private fun PrayerRowItem(row: PrayerRowUi) {
    val timeText = row.time ?: stringResource(R.string.time_placeholder)
    // Screen readers say "not calculated yet" instead of reading out "--:--".
    val spokenTime = row.time ?: stringResource(R.string.time_not_calculated)
    val textColor = if (row.isObligatory) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(row.nameRes),
                style = if (row.isObligatory) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = textColor,
            )
            if (!row.isObligatory) {
                Text(
                    text = stringResource(R.string.sunrise_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = timeText,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            modifier = Modifier.semantics { contentDescription = spokenTime },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrayerTimesReadyPreview() {
    UniversalAzanTheme {
        PrayerTimesScreen(
            state = PrayerTimesUiState.Ready(
                SavedLocation("London", 51.5085, -0.1257, "Europe/London", LocationSource.City),
                LocalDate.of(2026, 10, 12),
                notCalculatedPrayerRows(),
            ),
            onChooseLocation = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrayerTimesLoadingPreview() {
    UniversalAzanTheme {
        PrayerTimesScreen(PrayerTimesUiState.Loading, onChooseLocation = {}, onRetry = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PrayerTimesErrorPreview() {
    UniversalAzanTheme {
        PrayerTimesScreen(
            PrayerTimesUiState.Error(R.string.error_prayer_times),
            onChooseLocation = {},
            onRetry = {},
        )
    }
}
