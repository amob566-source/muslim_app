package com.universalazan.app.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalazan.app.R
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.domain.prayer.CalculationSettings
import com.universalazan.app.ui.common.ScreenContainer
import com.universalazan.app.ui.prayer.labelRes
import com.universalazan.app.ui.prayer.shortLabelRes
import com.universalazan.app.ui.theme.UniversalAzanTheme

@Composable
fun SettingsRoute(
    onOpenLocation: () -> Unit,
    onOpenCalculationSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val calculationSettings by viewModel.calculationSettings.collectAsStateWithLifecycle()
    SettingsScreen(
        currentLocation = currentLocation,
        calculationSettings = calculationSettings,
        onOpenLocation = onOpenLocation,
        onOpenCalculationSettings = onOpenCalculationSettings,
        modifier = modifier,
    )
}

/** Location and calculation settings work; the other groups are listed but not available yet. */
@Composable
fun SettingsScreen(
    currentLocation: SavedLocation?,
    calculationSettings: CalculationSettings,
    onOpenLocation: () -> Unit,
    onOpenCalculationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenContainer(title = stringResource(R.string.nav_settings), modifier = modifier) {
        SettingsLink(
            titleRes = R.string.settings_location_title,
            summaryRes = R.string.settings_location_summary,
            current = currentLocation?.let { stringResource(R.string.settings_location_selected, it.name) }
                ?: stringResource(R.string.settings_location_none),
            testTag = SETTINGS_LOCATION_TAG,
            onClick = onOpenLocation,
        )
        SettingsLink(
            titleRes = R.string.settings_calculation_title,
            summaryRes = R.string.settings_calculation_summary,
            current = stringResource(
                R.string.settings_calculation_current,
                stringResource(calculationSettings.method.labelRes),
                stringResource(calculationSettings.asrMadhab.shortLabelRes),
            ),
            testTag = SETTINGS_CALCULATION_TAG,
            onClick = onOpenCalculationSettings,
        )
        Text(
            text = stringResource(R.string.settings_other_coming_later),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsGroup(R.string.settings_azan_title, R.string.settings_azan_summary)
        SettingsGroup(R.string.settings_general_title, R.string.settings_general_summary)
    }
}

@Composable
private fun SettingsLink(
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    current: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(summaryRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = current,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SettingsGroup(@StringRes titleRes: Int, @StringRes summaryRes: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(summaryRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

const val SETTINGS_LOCATION_TAG = "settings_location"
const val SETTINGS_CALCULATION_TAG = "settings_calculation"

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    UniversalAzanTheme {
        SettingsScreen(
            currentLocation = null,
            calculationSettings = CalculationSettings(),
            onOpenLocation = {},
            onOpenCalculationSettings = {},
        )
    }
}
