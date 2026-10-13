package com.universalazan.app.domain.prayer

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.HighLatitudeRule
import com.batoulapps.adhan2.Madhab

/**
 * Calculation methods offered to the user. Each maps to one of the Adhan library's methods.
 * No single method is correct for every community; users should follow their local mosque.
 */
enum class PrayerCalculationMethod(val adhan: CalculationMethod) {
    MuslimWorldLeague(CalculationMethod.MUSLIM_WORLD_LEAGUE),
    Egyptian(CalculationMethod.EGYPTIAN),
    Karachi(CalculationMethod.KARACHI),
    UmmAlQura(CalculationMethod.UMM_AL_QURA),
    Dubai(CalculationMethod.DUBAI),
    MoonsightingCommittee(CalculationMethod.MOON_SIGHTING_COMMITTEE),
    NorthAmerica(CalculationMethod.NORTH_AMERICA),
    Kuwait(CalculationMethod.KUWAIT),
    Qatar(CalculationMethod.QATAR),
    Singapore(CalculationMethod.SINGAPORE),
    Turkey(CalculationMethod.TURKEY),
}

/** How the Asr time is worked out. */
enum class AsrMadhab(val adhan: Madhab) {
    /** Shadow equals object length (Shafi'i, Maliki, Hanbali). */
    Standard(Madhab.SHAFI),

    /** Shadow equals twice the object length. */
    Hanafi(Madhab.HANAFI),
}

/**
 * Limits on Fajr and Isha where the sun does not go deep enough below the horizon
 * (for example northern Europe in summer).
 */
enum class HighLatitudeOption(val adhan: HighLatitudeRule?) {
    /** Let the library choose: one-seventh of the night above 48° latitude, otherwise middle of the night. */
    Automatic(null),
    MiddleOfTheNight(HighLatitudeRule.MIDDLE_OF_THE_NIGHT),
    SeventhOfTheNight(HighLatitudeRule.SEVENTH_OF_THE_NIGHT),
    TwilightAngle(HighLatitudeRule.TWILIGHT_ANGLE),
}

/** Extra minutes added to (or, if negative, taken from) each calculated time. */
data class PrayerAdjustments(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0,
) {
    init {
        listOf(fajr, sunrise, dhuhr, asr, maghrib, isha).forEach {
            require(it in MIN_MINUTES..MAX_MINUTES) { "Adjustment must be between $MIN_MINUTES and $MAX_MINUTES minutes" }
        }
    }

    fun minutesFor(time: AdjustableTime): Int = when (time) {
        AdjustableTime.Fajr -> fajr
        AdjustableTime.Sunrise -> sunrise
        AdjustableTime.Dhuhr -> dhuhr
        AdjustableTime.Asr -> asr
        AdjustableTime.Maghrib -> maghrib
        AdjustableTime.Isha -> isha
    }

    /** A copy with [time] changed by [deltaMinutes], kept within the allowed range. */
    fun changedBy(time: AdjustableTime, deltaMinutes: Int): PrayerAdjustments {
        val value = (minutesFor(time) + deltaMinutes).coerceIn(MIN_MINUTES, MAX_MINUTES)
        return when (time) {
            AdjustableTime.Fajr -> copy(fajr = value)
            AdjustableTime.Sunrise -> copy(sunrise = value)
            AdjustableTime.Dhuhr -> copy(dhuhr = value)
            AdjustableTime.Asr -> copy(asr = value)
            AdjustableTime.Maghrib -> copy(maghrib = value)
            AdjustableTime.Isha -> copy(isha = value)
        }
    }

    companion object {
        const val MIN_MINUTES = -60
        const val MAX_MINUTES = 60
    }
}

/** The times that can be adjusted: the five prayers plus Sunrise (shown for information). */
enum class AdjustableTime { Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha }

data class CalculationSettings(
    val method: PrayerCalculationMethod = PrayerCalculationMethod.MuslimWorldLeague,
    val asrMadhab: AsrMadhab = AsrMadhab.Standard,
    val highLatitude: HighLatitudeOption = HighLatitudeOption.Automatic,
    val adjustments: PrayerAdjustments = PrayerAdjustments(),
)
