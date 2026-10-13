package com.universalazan.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalazan.app.R
import com.universalazan.app.domain.prayer.AdjustableTime
import com.universalazan.app.domain.prayer.AsrMadhab
import com.universalazan.app.domain.prayer.CalculationSettings
import com.universalazan.app.domain.prayer.HighLatitudeOption
import com.universalazan.app.domain.prayer.PrayerAdjustments
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
        onAdjust = viewModel::changeAdjustment,
        onReset = viewModel::resetToDefaults,
        modifier = modifier,
    )
}

/**
 * Choose calculation method, Asr time, high-latitude rule and per-prayer minute adjustments.
 * Changes are saved immediately.
 */
@Composable
fun CalculationSettingsScreen(
    settings: CalculationSettings,
    onBack: () -> Unit,
    onMethodSelected: (PrayerCalculationMethod) -> Unit,
    onAsrSelected: (AsrMadhab) -> Unit,
    onHighLatitudeSelected: (HighLatitudeOption) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    onAdjust: (AdjustableTime, Int) -> Unit = { _, _ -> },
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
        AdjustmentsSection(settings.adjustments, onAdjust)
        OutlinedButton(onClick = onReset) { Text(stringResource(R.string.calc_reset)) }
    }
}

@Composable
private fun AdjustmentsSection(adjustments: PrayerAdjustments, onAdjust: (AdjustableTime, Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.calc_adjustments_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.calc_adjustments_note), style = MaterialTheme.typography.bodyMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                AdjustableTime.entries.forEachIndexed { index, time ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AdjustmentRow(time, adjustments.minutesFor(time), onAdjust)
                }
            }
        }
    }
}

@Composable
private fun AdjustmentRow(time: AdjustableTime, minutes: Int, onAdjust: (AdjustableTime, Int) -> Unit) {
    val name = stringResource(time.labelRes)
    val valueText = stringResource(R.string.calc_adjustment_value, if (minutes > 0) "+$minutes" else "$minutes")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        val decrease = stringResource(R.string.calc_adjustment_decrease, name)
        val increase = stringResource(R.string.calc_adjustment_increase, name)
        OutlinedIconButtonText(
            text = "−",
            description = decrease,
            enabled = minutes > PrayerAdjustments.MIN_MINUTES,
            tag = adjustmentTag(time, "minus"),
            onClick = { onAdjust(time, -1) },
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 72.dp)
                .testTag(adjustmentTag(time, "value")),
        )
        OutlinedIconButtonText(
            text = "+",
            description = increase,
            enabled = minutes < PrayerAdjustments.MAX_MINUTES,
            tag = adjustmentTag(time, "plus"),
            onClick = { onAdjust(time, 1) },
        )
    }
}

@Composable
private fun OutlinedIconButtonText(text: String, description: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .size(48.dp)
            .testTag(tag),
    ) {
        // Label on the inner text so it merges into the button's own accessibility node.
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { contentDescription = description })
    }
}

private val AdjustableTime.labelRes: Int
    get() = when (this) {
        AdjustableTime.Fajr -> R.string.prayer_fajr
        AdjustableTime.Sunrise -> R.string.prayer_sunrise
        AdjustableTime.Dhuhr -> R.string.prayer_dhuhr
        AdjustableTime.Asr -> R.string.prayer_asr
        AdjustableTime.Maghrib -> R.string.prayer_maghrib
        AdjustableTime.Isha -> R.string.prayer_isha
    }

/** Stable identifier for tests, e.g. "adjust_Fajr_plus". */
fun adjustmentTag(time: AdjustableTime, part: String) = "adjust_${time.name}_$part"

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
