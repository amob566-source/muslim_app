package com.universalazan.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Whether the Azan should play automatically at prayer times. On by default. */
class AlarmSettingsRepository(private val dataStore: DataStore<Preferences>) {

    val enabled: Flow<Boolean> = dataStore.data.map { it[Enabled] ?: true }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[Enabled] = enabled }
    }

    suspend fun resetToDefaults() {
        dataStore.edit { it.remove(Enabled) }
    }

    private companion object {
        val Enabled = booleanPreferencesKey("alarms_enabled")
    }
}
