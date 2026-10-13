package com.universalazan.app.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalazan.app.R
import com.universalazan.app.audio.AzanSound
import com.universalazan.app.audio.AzanSoundSettings
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.data.preferences.GeneralSettings
import com.universalazan.app.data.preferences.ThemeSetting
import com.universalazan.app.data.preferences.TimeFormatSetting
import com.universalazan.app.domain.prayer.CalculationSettings
import com.universalazan.app.domain.prayer.Prayer
import com.universalazan.app.ui.common.ScreenContainer
import com.universalazan.app.ui.prayer.labelRes
import com.universalazan.app.ui.prayer.shortLabelRes
import com.universalazan.app.ui.theme.UniversalAzanTheme

@Composable
fun SettingsRoute(
    onOpenLocation: () -> Unit,
    onOpenCalculationSettings: () -> Unit,
    onOpenAzanSounds: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val calculationSettings by viewModel.calculationSettings.collectAsStateWithLifecycle()
    val azanSounds by viewModel.azanSounds.collectAsStateWithLifecycle()
    val alarmsEnabled by viewModel.alarmsEnabled.collectAsStateWithLifecycle()
    val general by viewModel.general.collectAsStateWithLifecycle()
    SettingsScreen(
        currentLocation = currentLocation,
        calculationSettings = calculationSettings,
        azanSounds = azanSounds,
        alarmsEnabled = alarmsEnabled,
        general = general,
        onOpenLocation = onOpenLocation,
        onOpenCalculationSettings = onOpenCalculationSettings,
        onOpenAzanSounds = onOpenAzanSounds,
        onOpenAlarms = onOpenAlarms,
        onOpenAbout = onOpenAbout,
        onTimeFormatSelected = viewModel::setTimeFormat,
        onThemeSelected = viewModel::setTheme,
        modifier = modifier,
    )
}

/** All settings, grouped. Every choice is saved on the phone straight away. */
@Composable
fun SettingsScreen(
    currentLocation: SavedLocation?,
    calculationSettings: CalculationSettings,
    azanSounds: AzanSoundSettings,
    alarmsEnabled: Boolean,
    general: GeneralSettings,
    onOpenLocation: () -> Unit,
    onOpenCalculationSettings: () -> Unit,
    onOpenAzanSounds: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenAbout: () -> Unit,
    onTimeFormatSelected: (TimeFormatSetting) -> Unit,
    onThemeSelected: (ThemeSetting) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenContainer(title = stringResource(R.string.nav_settings), modifier = modifier) {
        GroupTitle(R.string.settings_group_prayer)
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

        GroupTitle(R.string.settings_group_azan)
        val counts = Prayer.entries.groupingBy { azanSounds.soundFor(it) }.eachCount()
        SettingsLink(
            titleRes = R.string.settings_azan_title,
            summaryRes = R.string.settings_azan_summary,
            current = stringResource(
                R.string.settings_azan_current,
                counts[AzanSound.Full] ?: 0,
                counts[AzanSound.Short] ?: 0,
                counts[AzanSound.Silent] ?: 0,
            ),
            testTag = SETTINGS_AZAN_TAG,
            onClick = onOpenAzanSounds,
        )
        SettingsLink(
            titleRes = R.string.settings_alarms_title,
            summaryRes = R.string.settings_alarms_summary,
            current = stringResource(if (alarmsEnabled) R.string.settings_alarms_on else R.string.settings_alarms_off),
            testTag = SETTINGS_ALARMS_TAG,
            onClick = onOpenAlarms,
        )

        GroupTitle(R.string.settings_group_general)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipChoice(
                    titleRes = R.string.settings_time_format_title,
                    options = TimeFormatSetting.entries,
                    selected = general.timeFormat,
                    labelRes = {
                        when (it) {
                            TimeFormatSetting.System -> R.string.settings_time_format_system
                            TimeFormatSetting.TwelveHour -> R.string.settings_time_format_12
                            TimeFormatSetting.TwentyFourHour -> R.string.settings_time_format_24
                        }
                    },
                    onSelect = onTimeFormatSelected,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ChipChoice(
                    titleRes = R.string.settings_theme_title,
                    options = ThemeSetting.entries,
                    selected = general.theme,
                    labelRes = {
                        when (it) {
                            ThemeSetting.System -> R.string.settings_theme_system
                            ThemeSetting.Light -> R.string.settings_theme_light
                            ThemeSetting.Dark -> R.string.settings_theme_dark
                        }
                    },
                    onSelect = onThemeSelected,
                )
            }
        }

        GroupTitle(R.string.settings_group_about)
        SettingsLink(
            titleRes = R.string.settings_about_title,
            summaryRes = R.string.settings_about_summary,
            current = null,
            testTag = SETTINGS_ABOUT_TAG,
            onClick = onOpenAbout,
        )
    }
}

@Composable
private fun GroupTitle(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp).semantics { heading() },
    )
}

@Composable
private fun <T : Enum<T>> ChipChoice(
    @StringRes titleRes: Int,
    options: List<T>,
    selected: T,
    labelRes: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(stringResource(labelRes(option))) },
                modifier = Modifier.testTag(generalChipTag(option)),
            )
        }
    }
}

@Composable
private fun SettingsLink(
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    current: String?,
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
            if (current != null) {
                Text(
                    text = current,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

const val SETTINGS_LOCATION_TAG = "settings_location"
const val SETTINGS_CALCULATION_TAG = "settings_calculation"
const val SETTINGS_AZAN_TAG = "settings_azan"
const val SETTINGS_ALARMS_TAG = "settings_alarms"
const val SETTINGS_ABOUT_TAG = "settings_about"

/** Stable identifier for tests, e.g. "general_TwentyFourHour" or "general_Dark". */
fun generalChipTag(option: Enum<*>) = "general_${option.name}"

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    UniversalAzanTheme {
        SettingsScreen(
            currentLocation = null,
            calculationSettings = CalculationSettings(),
            azanSounds = AzanSoundSettings(),
            alarmsEnabled = true,
            general = GeneralSettings(),
            onOpenLocation = {},
            onOpenCalculationSettings = {},
            onOpenAzanSounds = {},
            onOpenAlarms = {},
            onOpenAbout = {},
            onTimeFormatSelected = {},
            onThemeSelected = {},
        )
    }
}
