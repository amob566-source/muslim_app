package com.universalazan.app.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.universalazan.app.R
import com.universalazan.app.ui.common.ScreenContainer
import com.universalazan.app.ui.theme.UniversalAzanTheme

/** Stage 1: shows the planned setting groups. None of them can be changed yet. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    ScreenContainer(title = stringResource(R.string.nav_settings), modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_coming_later),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsGroup(R.string.settings_location_title, R.string.settings_location_summary)
        SettingsGroup(R.string.settings_calculation_title, R.string.settings_calculation_summary)
        SettingsGroup(R.string.settings_azan_title, R.string.settings_azan_summary)
        SettingsGroup(R.string.settings_general_title, R.string.settings_general_summary)
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    UniversalAzanTheme {
        SettingsScreen()
    }
}
