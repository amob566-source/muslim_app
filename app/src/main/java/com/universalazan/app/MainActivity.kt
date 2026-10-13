package com.universalazan.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalazan.app.data.preferences.GeneralSettings
import com.universalazan.app.data.preferences.ThemeSetting
import com.universalazan.app.ui.common.LocalTimeFormatSetting
import com.universalazan.app.ui.navigation.AppRoot
import com.universalazan.app.ui.theme.UniversalAzanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val generalSettings = (application as UniversalAzanApp).container.generalSettingsRepository.settings
        setContent {
            // Null until the saved settings are read (a few milliseconds); nothing is drawn meanwhile,
            // so the screen never flashes in the wrong theme.
            val settings: GeneralSettings? by generalSettings.collectAsStateWithLifecycle(initialValue = null)
            val current = settings ?: return@setContent
            val darkTheme = when (current.theme) {
                ThemeSetting.System -> isSystemInDarkTheme()
                ThemeSetting.Light -> false
                ThemeSetting.Dark -> true
            }
            // Status-bar and navigation-bar icons follow the app's theme, not only the phone's.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
                )
                onDispose {}
            }
            CompositionLocalProvider(LocalTimeFormatSetting provides current.timeFormat) {
                UniversalAzanTheme(darkTheme = darkTheme) {
                    AppRoot()
                }
            }
        }
    }

    private companion object {
        // The default scrims used by enableEdgeToEdge for 3-button navigation.
        val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
