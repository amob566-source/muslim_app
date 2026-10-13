package com.universalazan.app.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.universalazan.app.domain.prayer.AsrMadhab
import com.universalazan.app.domain.prayer.CalculationSettings
import com.universalazan.app.domain.prayer.HighLatitudeOption
import com.universalazan.app.domain.prayer.PrayerAdjustments
import com.universalazan.app.domain.prayer.PrayerCalculationMethod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CalculationSettingsRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.newDataStore() =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "settings.preferences_pb") }

    @Test
    fun defaultsWhenNothingSaved() = runTest {
        assertEquals(CalculationSettings(), CalculationSettingsRepository(newDataStore()).settings.first())
    }

    @Test
    fun savesEachSetting() = runTest {
        val repository = CalculationSettingsRepository(newDataStore())
        val adjustments = PrayerAdjustments(fajr = 10, isha = -5)
        repository.setMethod(PrayerCalculationMethod.UmmAlQura)
        repository.setAsrMadhab(AsrMadhab.Hanafi)
        repository.setHighLatitude(HighLatitudeOption.TwilightAngle)
        repository.setAdjustments(adjustments)
        assertEquals(
            CalculationSettings(
                method = PrayerCalculationMethod.UmmAlQura,
                asrMadhab = AsrMadhab.Hanafi,
                highLatitude = HighLatitudeOption.TwilightAngle,
                adjustments = adjustments,
            ),
            repository.settings.first(),
        )
    }

    @Test
    fun resetRestoresDefaults() = runTest {
        val repository = CalculationSettingsRepository(newDataStore())
        repository.setMethod(PrayerCalculationMethod.Karachi)
        repository.setAsrMadhab(AsrMadhab.Hanafi)
        repository.resetToDefaults()
        assertEquals(CalculationSettings(), repository.settings.first())
    }

    @Test
    fun damagedValuesFallBackSafely() = runTest {
        val dataStore = newDataStore()
        dataStore.edit {
            it[stringPreferencesKey("calc_method")] = "NoSuchMethod"
            it[stringPreferencesKey("calc_asr_madhab")] = ""
            it[intPreferencesKey("adjust_fajr")] = 999
            it[intPreferencesKey("adjust_isha")] = -999
        }
        val settings = CalculationSettingsRepository(dataStore).settings.first()
        assertEquals(PrayerCalculationMethod.MuslimWorldLeague, settings.method)
        assertEquals(AsrMadhab.Standard, settings.asrMadhab)
        assertEquals(PrayerAdjustments.MAX_MINUTES, settings.adjustments.fajr)
        assertEquals(PrayerAdjustments.MIN_MINUTES, settings.adjustments.isha)
    }
}
