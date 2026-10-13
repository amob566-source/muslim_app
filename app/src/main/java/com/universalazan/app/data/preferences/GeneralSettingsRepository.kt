package com.universalazan.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How clock times are shown. */
enum class TimeFormatSetting { System, TwelveHour, TwentyFourHour }

/** Light or dark colours. */
enum class ThemeSetting { System, Light, Dark }

data class GeneralSettings(
    val timeFormat: TimeFormatSetting = TimeFormatSetting.System,
    val theme: ThemeSetting = ThemeSetting.System,
)

/** Saves the general display settings. Unknown values fall back to "follow the phone". */
class GeneralSettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<GeneralSettings> = dataStore.data.map { prefs ->
        GeneralSettings(
            timeFormat = TimeFormatSetting.entries.firstOrNull { it.name == prefs[TimeFormat] } ?: TimeFormatSetting.System,
            theme = ThemeSetting.entries.firstOrNull { it.name == prefs[Theme] } ?: ThemeSetting.System,
        )
    }

    suspend fun setTimeFormat(value: TimeFormatSetting) {
        dataStore.edit { it[TimeFormat] = value.name }
    }

    suspend fun setTheme(value: ThemeSetting) {
        dataStore.edit { it[Theme] = value.name }
    }

    suspend fun resetToDefaults() {
        dataStore.edit {
            it.remove(TimeFormat)
            it.remove(Theme)
        }
    }

    private companion object {
        val TimeFormat = stringPreferencesKey("general_time_format")
        val Theme = stringPreferencesKey("general_theme")
    }
}
