package com.universalazan.app.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.universalazan.app.R
import com.universalazan.app.ui.common.ScreenContainer

/** App information, privacy statement, permissions, data sources and limitations. */
@Composable
fun AboutRoute(onClose: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val version = remember(context) { appVersionName(context) }
    AboutScreen(version = version, onBack = onClose, modifier = modifier)
}

@Composable
fun AboutScreen(version: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    ScreenContainer(title = stringResource(R.string.about_title), onBack = onBack, modifier = modifier) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.about_version, version),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(ABOUT_VERSION_TAG),
                )
                Text(stringResource(R.string.about_description), style = MaterialTheme.typography.bodyMedium)
            }
        }
        AboutSection(R.string.about_privacy_title, R.string.about_privacy_text)
        AboutSection(R.string.about_permissions_title, R.string.about_permissions_text)
        AboutSection(R.string.about_sources_title, R.string.about_sources_text)
        AboutSection(R.string.about_limits_title, R.string.about_limits_text)
    }
}

@Composable
private fun AboutSection(titleRes: Int, textRes: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(textRes), style = MaterialTheme.typography.bodyMedium)
    }
}

/** The installed version, e.g. "0.1.0", read from Android (no extra build settings needed). */
fun appVersionName(context: Context): String {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    return info.versionName ?: "?"
}

const val ABOUT_VERSION_TAG = "about_version"
