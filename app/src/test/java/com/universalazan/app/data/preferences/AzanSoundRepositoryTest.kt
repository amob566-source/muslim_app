package com.universalazan.app.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.universalazan.app.audio.AzanSound
import com.universalazan.app.audio.AzanSoundSettings
import com.universalazan.app.domain.prayer.Prayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AzanSoundRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.newDataStore() =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "settings.preferences_pb") }

    @Test
    fun defaultsToFullAzan() = runTest {
        assertEquals(AzanSoundSettings(), AzanSoundRepository(newDataStore()).settings.first())
    }

    @Test
    fun savesEachPrayerSeparately() = runTest {
        val repository = AzanSoundRepository(newDataStore())
        repository.setSound(Prayer.Dhuhr, AzanSound.Short)
        repository.setSound(Prayer.Asr, AzanSound.Silent)
        val settings = repository.settings.first()
        assertEquals(AzanSound.Full, settings.soundFor(Prayer.Fajr))
        assertEquals(AzanSound.Short, settings.soundFor(Prayer.Dhuhr))
        assertEquals(AzanSound.Silent, settings.soundFor(Prayer.Asr))
        assertEquals(AzanSound.Full, settings.soundFor(Prayer.Maghrib))
        assertEquals(AzanSound.Full, settings.soundFor(Prayer.Isha))
    }

    @Test
    fun resetRestoresDefaults() = runTest {
        val repository = AzanSoundRepository(newDataStore())
        repository.setSound(Prayer.Isha, AzanSound.Silent)
        repository.resetToDefaults()
        assertEquals(AzanSoundSettings(), repository.settings.first())
    }

    @Test
    fun damagedValueFallsBackToFull() = runTest {
        val dataStore = newDataStore()
        dataStore.edit { it[stringPreferencesKey("azan_sound_fajr")] = "Loud" }
        assertEquals(AzanSound.Full, AzanSoundRepository(dataStore).settings.first().soundFor(Prayer.Fajr))
    }
}
