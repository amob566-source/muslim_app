package com.universalazan.app.ui.prayer

import androidx.compose.foundation.background
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
import com.universalazan.app.domain.prayer.CalculationSettings
import com.universalazan.app.ui.common.ScreenContainer
import com.universalazan.app.ui.common.formatClockTime
import com.universalazan.app.ui.theme.UniversalAzanTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Entry point used by navigation. */
@Composable
fun PrayerTimesRoute(
    onChooseLocation: () -> Unit,
    onOpenCalculationSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrayerTimesViewModel = viewModel(factory = PrayerTimesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PrayerTimesScreen(
        state = state,
        onChooseLocation = onChooseLocation,
        onOpenCalculationSettings = onOpenCalculationSettings,
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
    onOpenCalculationSettings: () -> Unit = {},
) {
    ScreenContainer(title = stringResource(R.string.nav_prayer_times), modifier = modifier) {
        when (state) {
            PrayerTimesUiState.Loading -> LoadingContent()
            is PrayerTimesUiState.Error -> ErrorContent(state.messageRes, onRetry)
            is PrayerTimesUiState.Ready -> ReadyContent(state, onChooseLocation, onOpenCalculationSettings)
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
private fun ReadyContent(
    state: PrayerTimesUiState.Ready,
    onChooseLocation: () -> Unit,
    onOpenCalculationSettings: () -> Unit,
) {
    LocationCard(state.location, onChooseLocation)
    DateLine(state.date)
    // "No location" is already explained in the location card above.
    val problemRes = when (state.problem) {
        PrayerProblem.InvalidTimeZone -> R.string.problem_invalid_time_zone
        PrayerProblem.InvalidCoordinates -> R.string.problem_invalid_coordinates
        PrayerProblem.SunDoesNotRiseOrSet -> R.string.problem_sun
        PrayerProblem.NoLocation, null -> null
    }
    if (problemRes != null) ProblemNotice(problemRes)
    PrayerList(state.prayers, state.date)
    if (state.settings != null) {
        CalculationSummary(state.settings, onOpenCalculationSettings)
    }
}

@Composable
private fun CalculationSummary(settings: CalculationSettings, onOpenCalculationSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(
                R.string.calculation_summary,
                stringResource(settings.method.labelRes),
                stringResource(settings.asrMadhab.shortLabelRes),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.calculation_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onOpenCalculationSettings) {
            Text(stringResource(R.string.calculation_settings_button))
        }
    }
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
    val locale = LocalConfiguration.current.locales[0]
    val formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(date)
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
private fun ProblemNotice(messageRes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun PrayerList(prayers: List<PrayerRowUi>, date: LocalDate) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            prayers.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                PrayerRowItem(row, date)
            }
        }
    }
}

@Composable
private fun PrayerRowItem(row: PrayerRowUi, date: LocalDate) {
    val formattedTime = row.time?.let { formatPrayerTime(it, date) }
    val timeText = formattedTime ?: stringResource(R.string.time_placeholder)
    // Screen readers say "not calculated yet" instead of reading out "--:--".
    val spokenTime = buildString {
        append(formattedTime ?: stringResource(R.string.time_not_calculated))
        if (row.isNext) append(", ").append(stringResource(R.string.next_prayer_spoken))
    }
    val textColor = when {
        row.isNext -> MaterialTheme.colorScheme.onPrimaryContainer
        row.isObligatory -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (row.isNext) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier,
            )
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
            if (row.isNext) {
                Text(
                    text = stringResource(R.string.next_prayer),
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
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

/**
 * Formats a time in the chosen 12- or 24-hour style. Marks times that fall on another
 * calendar day (possible for Isha or Fajr in some places).
 */
@Composable
private fun formatPrayerTime(time: ZonedDateTime, date: LocalDate): String {
    val text = formatClockTime(time)
    return when {
        time.toLocalDate().isAfter(date) -> text + " " + stringResource(R.string.time_next_day)
        time.toLocalDate().isBefore(date) -> text + " " + stringResource(R.string.time_previous_day)
        else -> text
    }
}

@Preview(showBackground = true)
@Composable
private fun PrayerTimesReadyPreview() {
    UniversalAzanTheme {
        PrayerTimesScreen(
            state = PrayerTimesViewModel.buildState(
                SavedLocation("London", 51.5085, -0.1257, "Europe/London", LocationSource.City),
                CalculationSettings(),
                Instant.parse("2026-10-13T13:00:00Z"),
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
