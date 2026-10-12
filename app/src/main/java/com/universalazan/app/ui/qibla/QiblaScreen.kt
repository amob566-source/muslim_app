package com.universalazan.app.ui.qibla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.universalazan.app.R
import com.universalazan.app.ui.common.ScreenContainer
import com.universalazan.app.ui.theme.UniversalAzanTheme

/** Stage 1: no bearing or compass yet, so this screen says so plainly. */
@Composable
fun QiblaScreen(
    onChooseLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenContainer(title = stringResource(R.string.nav_qibla), modifier = modifier) {
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
                    text = stringResource(R.string.qibla_not_available),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.qibla_not_available_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onChooseLocation) {
                    Text(stringResource(R.string.choose_location))
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

@Preview(showBackground = true)
@Composable
private fun QiblaScreenPreview() {
    UniversalAzanTheme {
        QiblaScreen(onChooseLocation = {})
    }
}
