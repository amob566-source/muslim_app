package com.universalazan.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.universalazan.app.domain.prayer.AsrMadhab
import com.universalazan.app.domain.prayer.CalculationSettings
import com.universalazan.app.domain.prayer.HighLatitudeOption
import com.universalazan.app.domain.prayer.PrayerAdjustments
import com.universalazan.app.domain.prayer.PrayerCalculationMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Saves and reads the prayer-calculation settings. Unknown or damaged values fall back to defaults. */
class CalculationSettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<CalculationSettings> = dataStore.data.map { it.toSettings() }

    suspend fun setMethod(method: PrayerCalculationMethod) {
        dataStore.edit { it[Keys.Method] = method.name }
    }

    suspend fun setAsrMadhab(madhab: AsrMadhab) {
        dataStore.edit { it[Keys.AsrMadhab] = madhab.name }
    }

    suspend fun setHighLatitude(option: HighLatitudeOption) {
        dataStore.edit { it[Keys.HighLatitude] = option.name }
    }

    suspend fun setAdjustments(adjustments: PrayerAdjustments) {
        dataStore.edit {
            it[Keys.AdjustFajr] = adjustments.fajr
            it[Keys.AdjustSunrise] = adjustments.sunrise
            it[Keys.AdjustDhuhr] = adjustments.dhuhr
            it[Keys.AdjustAsr] = adjustments.asr
            it[Keys.AdjustMaghrib] = adjustments.maghrib
            it[Keys.AdjustIsha] = adjustments.isha
        }
    }

    suspend fun resetToDefaults() {
        dataStore.edit { prefs -> Keys.all.forEach { prefs.remove(it) } }
    }

    private fun Preferences.toSettings(): CalculationSettings {
        val defaults = CalculationSettings()
        fun minutes(key: Preferences.Key<Int>) =
            (this[key] ?: 0).coerceIn(PrayerAdjustments.MIN_MINUTES, PrayerAdjustments.MAX_MINUTES)
        return CalculationSettings(
            method = enumOrDefault(this[Keys.Method], defaults.method),
            asrMadhab = enumOrDefault(this[Keys.AsrMadhab], defaults.asrMadhab),
            highLatitude = enumOrDefault(this[Keys.HighLatitude], defaults.highLatitude),
            adjustments = PrayerAdjustments(
                fajr = minutes(Keys.AdjustFajr),
                sunrise = minutes(Keys.AdjustSunrise),
                dhuhr = minutes(Keys.AdjustDhuhr),
                asr = minutes(Keys.AdjustAsr),
                maghrib = minutes(Keys.AdjustMaghrib),
                isha = minutes(Keys.AdjustIsha),
            ),
        )
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val Method = stringPreferencesKey("calc_method")
        val AsrMadhab = stringPreferencesKey("calc_asr_madhab")
        val HighLatitude = stringPreferencesKey("calc_high_latitude")
        val AdjustFajr = intPreferencesKey("adjust_fajr")
        val AdjustSunrise = intPreferencesKey("adjust_sunrise")
        val AdjustDhuhr = intPreferencesKey("adjust_dhuhr")
        val AdjustAsr = intPreferencesKey("adjust_asr")
        val AdjustMaghrib = intPreferencesKey("adjust_maghrib")
        val AdjustIsha = intPreferencesKey("adjust_isha")
        val all = listOf(Method, AsrMadhab, HighLatitude, AdjustFajr, AdjustSunrise, AdjustDhuhr, AdjustAsr, AdjustMaghrib, AdjustIsha)
    }
}
