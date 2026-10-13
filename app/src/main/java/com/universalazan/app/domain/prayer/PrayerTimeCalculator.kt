package com.universalazan.app.domain.prayer

import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.universalazan.app.domain.location.isValidLatitude
import com.universalazan.app.domain.location.isValidLongitude
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import com.batoulapps.adhan2.PrayerAdjustments as AdhanAdjustments

/** One day's times, already converted to the location's own time zone. */
data class DailyPrayerTimes(
    val date: LocalDate,
    val zone: ZoneId,
    val fajr: ZonedDateTime,
    /** For information only; sunrise is not a prayer. */
    val sunrise: ZonedDateTime,
    val dhuhr: ZonedDateTime,
    val asr: ZonedDateTime,
    val maghrib: ZonedDateTime,
    val isha: ZonedDateTime,
)

sealed interface PrayerCalculationResult {
    data class Success(val times: DailyPrayerTimes) : PrayerCalculationResult
    data object InvalidCoordinates : PrayerCalculationResult

    /**
     * The sun does not rise or set at this place on this date (polar day or night),
     * so the times cannot be calculated with the usual rules.
     */
    data object SunDoesNotRiseOrSet : PrayerCalculationResult
}

/**
 * Calculates prayer times on the device with the Adhan library. No network is used.
 *
 * The date is the calendar date *at the location*, and results are converted with the
 * location's [ZoneId], so daylight-saving rules are applied by java.time, never by hand.
 */
object PrayerTimeCalculator {

    fun calculate(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        zone: ZoneId,
        settings: CalculationSettings = CalculationSettings(),
    ): PrayerCalculationResult {
        if (!isValidLatitude(latitude) || !isValidLongitude(longitude)) {
            return PrayerCalculationResult.InvalidCoordinates
        }
        val a = settings.adjustments
        val parameters = settings.method.adhan.parameters.copy(
            madhab = settings.asrMadhab.adhan,
            highLatitudeRule = settings.highLatitude.adhan,
            prayerAdjustments = AdhanAdjustments(a.fajr, a.sunrise, a.dhuhr, a.asr, a.maghrib, a.isha),
        )
        val coordinates = Coordinates(latitude, longitude)

        var times = compute(coordinates, date, zone, parameters)
            ?: return PrayerCalculationResult.SunDoesNotRiseOrSet
        // Adhan works on the solar day at the given longitude. A few zones (e.g. Kiritimati,
        // UTC+14 at 157° W) run about a day ahead of the sun, so that solar day's Dhuhr lands on
        // a different local date. Shift the solar day so Dhuhr falls on the requested date.
        val dhuhrDate = times.dhuhr.toLocalDate()
        if (dhuhrDate != date) {
            val shift = ChronoUnit.DAYS.between(dhuhrDate, date)
            times = compute(coordinates, date.plusDays(shift), zone, parameters)
                ?: return PrayerCalculationResult.SunDoesNotRiseOrSet
        }
        return PrayerCalculationResult.Success(times.copy(date = date))
    }

    /** Returns null if Adhan cannot calculate this day (no sunrise or sunset). */
    private fun compute(
        coordinates: Coordinates,
        solarDate: LocalDate,
        zone: ZoneId,
        parameters: CalculationParameters,
    ): DailyPrayerTimes? {
        // Built directly from the date. (DateComponents.from(instant) would use the phone's
        // time zone, which is wrong when the selected city is elsewhere.)
        val dateComponents = DateComponents(solarDate.year, solarDate.monthValue, solarDate.dayOfMonth)
        val times = try {
            PrayerTimes(coordinates, dateComponents, parameters)
        } catch (_: IllegalStateException) {
            // Adhan throws when sunrise or sunset does not exist on this date.
            return null
        }
        fun local(instant: kotlin.time.Instant): ZonedDateTime =
            Instant.ofEpochMilli(instant.toEpochMilliseconds()).atZone(zone)

        return DailyPrayerTimes(
            date = solarDate,
            zone = zone,
            fajr = local(times.fajr),
            sunrise = local(times.sunrise),
            dhuhr = local(times.dhuhr),
            asr = local(times.asr),
            maghrib = local(times.maghrib),
            isha = local(times.isha),
        )
    }
}
