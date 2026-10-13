package com.universalazan.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalazan.app.R
import com.universalazan.app.alarms.PlannedAlarm
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.ui.common.ScreenContainer
import com.universalazan.app.ui.common.formatClockTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun AlarmsRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlarmsViewModel = viewModel(factory = AlarmsViewModel.Factory),
) {
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val upcoming by viewModel.upcoming.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val testAlarmAt by viewModel.testAlarmAt.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    BackHandler(onBack = onClose)
    // Permissions can be changed in system settings while the app is in the background.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.refresh()
        val askedBefore = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        // Android will no longer show the request: send the user to the app's notification settings.
        if (!granted && !askedBefore) openNotificationSettings(context)
    }

    AlarmsScreen(
        enabled = enabled,
        location = location,
        upcoming = upcoming,
        status = status,
        testAlarmAt = testAlarmAt,
        onBack = onClose,
        onEnabledChange = viewModel::setEnabled,
        onAllowNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings(context)
            }
        },
        onAllowExactAlarms = { openExactAlarmSettings(context) },
        onOpenBatterySettings = { openBatterySettings(context) },
        onTestAlarm = viewModel::scheduleTestAlarm,
        modifier = modifier,
    )
}

@Composable
fun AlarmsScreen(
    enabled: Boolean,
    location: SavedLocation?,
    upcoming: List<PlannedAlarm>,
    status: AlarmSystemStatus,
    testAlarmAt: Instant?,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onAllowNotifications: () -> Unit,
    onAllowExactAlarms: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onTestAlarm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenContainer(title = stringResource(R.string.alarms_title), onBack = onBack, modifier = modifier) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange)
                    .testTag(ALARMS_SWITCH_TAG)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.alarms_switch), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.alarms_switch_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = null)
            }
        }
        if (location == null) {
            Text(stringResource(R.string.alarms_no_location), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        SectionTitle(stringResource(R.string.alarms_checks_title))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusItem(
                    ok = status.exactAlarmsAllowed,
                    okText = stringResource(R.string.alarms_exact_ok),
                    problemText = stringResource(R.string.alarms_exact_missing),
                    buttonText = stringResource(R.string.alarms_exact_button),
                    onFix = onAllowExactAlarms,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatusItem(
                    ok = status.notificationsAllowed,
                    okText = stringResource(R.string.alarms_notifications_ok),
                    problemText = stringResource(R.string.alarms_notifications_missing),
                    buttonText = stringResource(R.string.alarms_notifications_button),
                    onFix = onAllowNotifications,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatusItem(
                    ok = !status.alarmVolumeMuted,
                    okText = stringResource(R.string.alarms_volume_ok),
                    problemText = stringResource(R.string.alarms_volume_muted),
                    buttonText = null,
                    onFix = {},
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatusItem(
                    ok = status.batteryUnrestricted,
                    okText = stringResource(R.string.alarms_battery_ok),
                    problemText = stringResource(R.string.alarms_battery_restricted),
                    buttonText = stringResource(R.string.alarms_battery_button),
                    onFix = onOpenBatterySettings,
                    isWarningOnly = true,
                )
            }
        }

        SectionTitle(stringResource(R.string.alarms_upcoming_title))
        Card(modifier = Modifier.fillMaxWidth().testTag(ALARMS_UPCOMING_TAG)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (upcoming.isEmpty()) {
                    Text(stringResource(R.string.alarms_upcoming_none), style = MaterialTheme.typography.bodyMedium)
                } else {
                    val locale = LocalConfiguration.current.locales[0]
                    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
                    val zone = location?.zoneId ?: ZoneId.systemDefault()
                    upcoming.forEach { alarm ->
                        val at = alarm.time.atZone(zone)
                        Text(
                            text = stringResource(
                                R.string.alarms_upcoming_item,
                                stringResource(alarm.prayer.labelRes),
                                dateFormatter.format(at) + ", " + formatClockTime(at),
                            ) + " · " + stringResource(alarm.sound.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        OutlinedButton(onClick = onTestAlarm, modifier = Modifier.testTag(ALARMS_TEST_TAG)) {
            Text(stringResource(R.string.alarms_test_button))
        }
        Text(stringResource(R.string.alarms_test_explanation), style = MaterialTheme.typography.bodySmall)
        if (testAlarmAt != null) {
            val time = formatClockTime(testAlarmAt.atZone(ZoneId.systemDefault()))
            Text(stringResource(R.string.alarms_test_set, time), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }

        SectionTitle(stringResource(R.string.alarms_limits_title))
        Text(stringResource(R.string.alarms_limits_text), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun StatusItem(
    ok: Boolean,
    okText: String,
    problemText: String,
    buttonText: String?,
    onFix: () -> Unit,
    isWarningOnly: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (ok) {
            Text("✓ $okText", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                text = problemText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isWarningOnly) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
            )
            if (buttonText != null) {
                OutlinedButton(onClick = onFix) { Text(buttonText) }
            }
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    runCatching { context.startActivity(intent) }
}

private fun openExactAlarmSettings(context: Context) {
    val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    // The "Alarms and reminders" page exists from Android 12; before that no permission is needed.
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null))
    } else {
        appDetails
    }
    runCatching { context.startActivity(intent) }.onFailure { runCatching { context.startActivity(appDetails) } }
}

private fun openBatterySettings(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
}

const val ALARMS_SWITCH_TAG = "alarms_switch"
const val ALARMS_UPCOMING_TAG = "alarms_upcoming"
const val ALARMS_TEST_TAG = "alarms_test"
