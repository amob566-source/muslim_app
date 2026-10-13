package com.universalazan.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalazan.app.R
import com.universalazan.app.domain.prayer.AsrMadhab
import com.universalazan.app.domain.prayer.CalculationSettings
import com.universalazan.app.domain.prayer.HighLatitudeOption
import com.universalazan.app.domain.prayer.PrayerCalculationMethod
import com.universalazan.app.ui.common.ScreenContainer
import com.universalazan.app.ui.prayer.descriptionRes
import com.universalazan.app.ui.prayer.labelRes

@Composable
fun CalculationSettingsRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalculationSettingsViewModel = viewModel(factory = CalculationSettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    BackHandler(onBack = onClose)
    CalculationSettingsScreen(
        settings = settings,
        onBack = onClose,
        onMethodSelected = viewModel::setMethod,
        onAsrSelected = viewModel::setAsrMadhab,
        onHighLatitudeSelected = viewModel::setHighLatitude,
        onReset = viewModel::resetToDefaults,
        modifier = modifier,
    )
}

/** Choose calculation method, Asr time and high-latitude rule. Changes are saved immediately. */
@Composable
fun CalculationSettingsScreen(
    settings: CalculationSettings,
    onBack: () -> Unit,
    onMethodSelected: (PrayerCalculationMethod) -> Unit,
    onAsrSelected: (AsrMadhab) -> Unit,
    onHighLatitudeSelected: (HighLatitudeOption) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenContainer(
        title = stringResource(R.string.calc_settings_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        ChoiceGroup(
            title = stringResource(R.string.calc_method_title),
            note = stringResource(R.string.calc_method_note),
            options = PrayerCalculationMethod.entries,
            selected = settings.method,
            labelRes = { it.labelRes },
            descriptionRes = { it.descriptionRes },
            onSelect = onMethodSelected,
        )
        ChoiceGroup(
            title = stringResource(R.string.calc_asr_title),
            note = null,
            options = AsrMadhab.entries,
            selected = settings.asrMadhab,
            labelRes = { it.labelRes },
            descriptionRes = { it.descriptionRes },
            onSelect = onAsrSelected,
        )
        ChoiceGroup(
            title = stringResource(R.string.calc_high_lat_title),
            note = stringResource(R.string.calc_high_lat_note),
            options = HighLatitudeOption.entries,
            selected = settings.highLatitude,
            labelRes = { it.labelRes },
            descriptionRes = { it.descriptionRes },
            onSelect = onHighLatitudeSelected,
        )
        Text(
            text = stringResource(R.string.calc_adjustments_later),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onReset) { Text(stringResource(R.string.calc_reset)) }
    }
}

@Composable
private fun <T : Enum<T>> ChoiceGroup(
    title: String,
    note: String?,
    options: List<T>,
    selected: T,
    labelRes: (T) -> Int,
    descriptionRes: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        if (note != null) {
            Text(note, style = MaterialTheme.typography.bodyMedium)
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEachIndexed { index, option ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selected,
                                onClick = { onSelect(option) },
                                role = Role.RadioButton,
                            )
                            .testTag(choiceTestTag(option))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The whole row is the click target; the radio button only shows the state.
                        RadioButton(selected = option == selected, onClick = null)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                        ) {
                            Text(stringResource(labelRes(option)), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(descriptionRes(option)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Stable identifier so automated tests can find each option, e.g. "choice_Hanafi". */
fun choiceTestTag(option: Enum<*>): String = "choice_${option.name}"
