package com.universalazan.app.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GeneralSettingsRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.newDataStore() =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "settings.preferences_pb") }

    @Test
    fun defaultsFollowThePhone() = runTest {
        assertEquals(GeneralSettings(TimeFormatSetting.System, ThemeSetting.System), GeneralSettingsRepository(newDataStore()).settings.first())
    }

    @Test
    fun savesTimeFormatAndTheme() = runTest {
        val repository = GeneralSettingsRepository(newDataStore())
        repository.setTimeFormat(TimeFormatSetting.TwentyFourHour)
        repository.setTheme(ThemeSetting.Dark)
        assertEquals(GeneralSettings(TimeFormatSetting.TwentyFourHour, ThemeSetting.Dark), repository.settings.first())
    }

    @Test
    fun resetRestoresDefaults() = runTest {
        val repository = GeneralSettingsRepository(newDataStore())
        repository.setTheme(ThemeSetting.Light)
        repository.resetToDefaults()
        assertEquals(GeneralSettings(), repository.settings.first())
    }

    @Test
    fun damagedValuesFallBackToPhoneSetting() = runTest {
        val dataStore = newDataStore()
        dataStore.edit {
            it[stringPreferencesKey("general_time_format")] = "Sundial"
            it[stringPreferencesKey("general_theme")] = ""
        }
        assertEquals(GeneralSettings(), GeneralSettingsRepository(dataStore).settings.first())
    }
}
