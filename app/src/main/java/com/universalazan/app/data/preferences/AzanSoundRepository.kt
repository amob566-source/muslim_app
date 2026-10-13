package com.universalazan.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.universalazan.app.audio.AzanSound
import com.universalazan.app.audio.AzanSoundSettings
import com.universalazan.app.domain.prayer.Prayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Saves the Azan sound chosen for each prayer. Unknown values fall back to the full Azan. */
class AzanSoundRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AzanSoundSettings> = dataStore.data.map { prefs ->
        AzanSoundSettings(
            Prayer.entries.associateWith { prayer ->
                AzanSound.entries.firstOrNull { it.name == prefs[key(prayer)] } ?: AzanSound.Full
            },
        )
    }

    suspend fun setSound(prayer: Prayer, sound: AzanSound) {
        dataStore.edit { it[key(prayer)] = sound.name }
    }

    suspend fun resetToDefaults() {
        dataStore.edit { prefs -> Prayer.entries.forEach { prefs.remove(key(it)) } }
    }

    private fun key(prayer: Prayer) = stringPreferencesKey("azan_sound_${prayer.name.lowercase()}")
}
