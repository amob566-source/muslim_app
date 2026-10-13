package com.universalazan.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.universalazan.app.R
import com.universalazan.app.ui.location.LocationRoute
import com.universalazan.app.ui.prayer.PrayerTimesRoute
import com.universalazan.app.ui.qibla.QiblaRoute
import com.universalazan.app.ui.settings.AboutRoute
import com.universalazan.app.ui.settings.AlarmsRoute
import com.universalazan.app.ui.settings.AzanSoundsRoute
import com.universalazan.app.ui.settings.CalculationSettingsRoute
import com.universalazan.app.ui.settings.SettingsRoute

/** Screens shown on top of a section, with their own back button. */
private enum class Overlay { Location, CalculationSettings, AzanSounds, Alarms, About }

/** Width from which a side rail replaces the bottom bar (landscape phones, tablets). */
private val WideLayoutMinWidth = 600.dp

/**
 * Top-level app layout and navigation between the three sections.
 * The selected section survives screen rotation (rememberSaveable).
 */
@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    var current by rememberSaveable { mutableStateOf(AppDestination.PrayerTimes) }
    // Sub-screens open on top of whichever section the user came from.
    var overlay by rememberSaveable { mutableStateOf<Overlay?>(null) }
    val navigate: (AppDestination) -> Unit = {
        current = it
        overlay = null
    }
    val openLocation = { overlay = Overlay.Location }
    val openCalculationSettings = { overlay = Overlay.CalculationSettings }
    val openAzanSounds = { overlay = Overlay.AzanSounds }
    val openAlarms = { overlay = Overlay.Alarms }
    val openAbout = { overlay = Overlay.About }
    val closeOverlay = { overlay = null }

    // Back from Qibla or Settings returns to Prayer Times; back from Prayer Times leaves the app.
    BackHandler(enabled = current != AppDestination.PrayerTimes) {
        current = AppDestination.PrayerTimes
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= WideLayoutMinWidth
        Row(modifier = Modifier.fillMaxSize()) {
            if (wide) {
                AppNavigationRail(current, navigate)
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = { AppTopBar() },
                bottomBar = {
                    if (!wide) AppNavigationBar(current, navigate)
                },
                // The rail already handles the start-side inset in wide layouts.
                contentWindowInsets = if (wide) {
                    ScaffoldDefaults.contentWindowInsets.only(
                        WindowInsetsSides.Vertical + WindowInsetsSides.End,
                    )
                } else {
                    ScaffoldDefaults.contentWindowInsets
                },
            ) { innerPadding ->
                val contentModifier = Modifier.padding(innerPadding)
                when (overlay) {
                    Overlay.Location -> LocationRoute(onClose = closeOverlay, modifier = contentModifier)
                    Overlay.CalculationSettings -> CalculationSettingsRoute(onClose = closeOverlay, modifier = contentModifier)
                    Overlay.AzanSounds -> AzanSoundsRoute(onClose = closeOverlay, modifier = contentModifier)
                    Overlay.Alarms -> AlarmsRoute(onClose = closeOverlay, modifier = contentModifier)
                    Overlay.About -> AboutRoute(onClose = closeOverlay, modifier = contentModifier)
                    null -> when (current) {
                        AppDestination.PrayerTimes -> PrayerTimesRoute(
                            onChooseLocation = openLocation,
                            onOpenCalculationSettings = openCalculationSettings,
                            modifier = contentModifier,
                        )
                        AppDestination.Qibla -> QiblaRoute(
                            onChooseLocation = openLocation,
                            modifier = contentModifier,
                        )
                        AppDestination.Settings -> SettingsRoute(
                            onOpenLocation = openLocation,
                            onOpenCalculationSettings = openCalculationSettings,
                            onOpenAzanSounds = openAzanSounds,
                            onOpenAlarms = openAlarms,
                            onOpenAbout = openAbout,
                            modifier = contentModifier,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTopBar() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.End),
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .semantics { heading() },
            )
            // Thin gold line under the title.
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun AppNavigationBar(current: AppDestination, onNavigate: (AppDestination) -> Unit) {
    NavigationBar {
        AppDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == current,
                onClick = { onNavigate(destination) },
                icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
                modifier = Modifier.testTag(navTestTag(destination)),
            )
        }
    }
}

@Composable
private fun AppNavigationRail(current: AppDestination, onNavigate: (AppDestination) -> Unit) {
    NavigationRail(modifier = Modifier.fillMaxHeight()) {
        AppDestination.entries.forEach { destination ->
            NavigationRailItem(
                selected = destination == current,
                onClick = { onNavigate(destination) },
                icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
                modifier = Modifier.testTag(navTestTag(destination)),
            )
        }
    }
}

/** Stable identifier so automated tests can find each navigation item. */
fun navTestTag(destination: AppDestination): String = "nav_${destination.name}"
