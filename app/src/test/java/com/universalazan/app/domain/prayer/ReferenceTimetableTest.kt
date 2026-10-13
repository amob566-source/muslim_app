package com.universalazan.app.domain.prayer

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Compares our results with published timetables. Sources and every difference found are
 * recorded in docs/prayer-time-validation.md.
 *
 * - JAKIM: official Malaysian timetable (e-solat.gov.my), zone WLY01 (Kuala Lumpur).
 * - AlAdhan (api.aladhan.com): an independent implementation of the same astronomy. Not an
 *   official timetable. Requested with the angle-based high-latitude rule unless noted.
 * - NOAA: Asr computed independently from the NOAA solar-position equations, used where
 *   AlAdhan's Asr was found to be 2-3 minutes off at London's latitude.
 *
 * Each expected time is local "HH:mm". Allowed difference is [TOLERANCE_MINUTES], except where a
 * known, documented difference is given; then the difference must stay at that value (± tolerance).
 */
class ReferenceTimetableTest {

    private val names = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")

    private fun check(
        label: String,
        latitude: Double,
        longitude: Double,
        date: String,
        zone: String,
        settings: CalculationSettings,
        expected: List<String>, // fajr, sunrise, dhuhr, asr, maghrib, isha
        knownDifferences: Map<String, Long> = emptyMap(),
    ) {
        val result = PrayerTimeCalculator.calculate(latitude, longitude, LocalDate.parse(date), ZoneId.of(zone), settings)
        if (result !is PrayerCalculationResult.Success) fail("$label: no result ($result)")
        val t = (result as PrayerCalculationResult.Success).times
        val actual = listOf(t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha)
        val problems = names.indices.mapNotNull { i ->
            val diff = minutesBetween(expected[i], actual[i])
            val allowedCentre = knownDifferences[names[i]] ?: 0L
            if (abs(diff - allowedCentre) > TOLERANCE_MINUTES) {
                "${names[i]} expected ${expected[i]} got ${actual[i].toLocalTime()} (${signed(diff)} min)"
            } else {
                null
            }
        }
        assertTrue("$label: ${problems.joinToString("; ")}", problems.isEmpty())
    }

    private fun minutesBetween(expected: String, actual: ZonedDateTime): Long {
        val expectedTime = actual.with(LocalTime.parse(expected))
        return ChronoUnit.MINUTES.between(expectedTime, actual.truncatedTo(ChronoUnit.MINUTES))
    }

    private fun signed(n: Long) = if (n > 0) "+$n" else "$n"

    private val singapore = CalculationSettings(method = PrayerCalculationMethod.Singapore)

    /** Same rule as the AlAdhan reference (latitudeAdjustmentMethod=3, angle-based). */
    private val mwlAngle = CalculationSettings(
        method = PrayerCalculationMethod.MuslimWorldLeague,
        highLatitude = HighLatitudeOption.TwilightAngle,
    )

    // --- Official: JAKIM, Kuala Lumpur (WLY01) ---
    // Known difference: JAKIM's Fajr is 10 minutes later than the 20° "Singapore" method.
    // Our Fajr equals JAKIM's published imsak time. All other times agree within 2 minutes.

    @Test
    fun jakimKualaLumpur2026Oct13() = check(
        "JAKIM KL 2026-10-13", 3.1390, 101.6869, "2026-10-13", "Asia/Kuala_Lumpur", singapore,
        listOf("05:51", "06:57", "13:02", "16:18", "19:03", "20:12"),
        knownDifferences = mapOf("Fajr" to -10L),
    )

    @Test
    fun jakimKualaLumpur2026Dec21() = check(
        "JAKIM KL 2026-12-21", 3.1390, 101.6869, "2026-12-21", "Asia/Kuala_Lumpur", singapore,
        listOf("06:01", "07:12", "13:14", "16:37", "19:11", "20:26"),
        knownDifferences = mapOf("Fajr" to -10L),
    )

    // --- AlAdhan cross-check, with Asr from NOAA where AlAdhan's Asr was 2-3 min late ---

    @Test
    fun londonMuslimWorldLeague() = check(
        "London MWL 2026-10-13", 51.5074, -0.1278, "2026-10-13", "Europe/London", mwlAngle,
        listOf("05:30", "07:21", "12:47", "15:36", "18:12", "19:56"),
    )

    @Test
    fun londonHanafiAsr() = check(
        "London MWL Hanafi 2026-10-13", 51.5074, -0.1278, "2026-10-13", "Europe/London",
        mwlAngle.copy(asrMadhab = AsrMadhab.Hanafi),
        listOf("05:30", "07:21", "12:47", "16:19", "18:12", "19:56"),
    )

    @Test
    fun londonDaylightSavingStartsMarch29() {
        // Clocks go forward one hour on 29 March 2026.
        check(
            "London 2026-03-28 (GMT)", 51.5074, -0.1278, "2026-03-28", "Europe/London", mwlAngle,
            listOf("03:49", "05:45", "12:06", "15:34", "18:27", "20:16"),
        )
        check(
            "London 2026-03-29 (BST)", 51.5074, -0.1278, "2026-03-29", "Europe/London", mwlAngle,
            listOf("04:46", "06:43", "13:05", "16:35", "19:29", "21:19"),
        )
    }

    @Test
    fun londonDaylightSavingEndsOctober25() {
        // Clocks go back one hour on 25 October 2026.
        check(
            "London 2026-10-24 (BST)", 51.5074, -0.1278, "2026-10-24", "Europe/London", mwlAngle,
            listOf("05:48", "07:40", "12:45", "15:18", "17:49", "19:34"),
        )
        check(
            "London 2026-10-25 (GMT)", 51.5074, -0.1278, "2026-10-25", "Europe/London", mwlAngle,
            listOf("04:49", "06:42", "11:45", "14:16", "16:47", "18:32"),
        )
    }

    @Test
    fun daylightSavingMovesClockTimesByAboutOneHour() {
        fun dhuhr(date: String) = (PrayerTimeCalculator.calculate(
            51.5074, -0.1278, LocalDate.parse(date), ZoneId.of("Europe/London"), mwlAngle,
        ) as PrayerCalculationResult.Success).times.dhuhr
        val before = dhuhr("2026-10-24")
        val after = dhuhr("2026-10-25")
        // The clock reading drops by ~1 hour, but solar noon is fixed in real (UTC) time,
        // so about 24 real hours pass between the two Dhuhr times.
        assertTrue(Duration.between(before.toLocalTime(), after.toLocalTime()).toMinutes() in -62L..-58L)
        assertTrue(Duration.between(before, after).toMinutes() in (24 * 60 - 2L)..(24 * 60 + 2L))
    }

    @Test
    fun londonMidsummerSeventhOfNight() = check(
        // AlAdhan with latitudeAdjustmentMethod=2 (one-seventh).
        "London 2026-06-21 1/7 night", 51.5074, -0.1278, "2026-06-21", "Europe/London",
        mwlAngle.copy(highLatitude = HighLatitudeOption.SeventhOfTheNight),
        listOf("03:40", "04:43", "13:02", "17:25", "21:22", "22:25"),
    )

    @Test
    fun automaticHighLatitudeRuleLimitsLondonFajrInLateMarch() {
        // Documented behaviour: above 48° the automatic (one-seventh) rule is used. On 28 March it
        // makes Fajr about 20 minutes later and Isha about 12 minutes earlier than the 18°/17° angles.
        check(
            "London 2026-03-28 automatic", 51.5074, -0.1278, "2026-03-28", "Europe/London",
            CalculationSettings(method = PrayerCalculationMethod.MuslimWorldLeague),
            listOf("03:49", "05:45", "12:06", "15:34", "18:27", "20:16"),
            knownDifferences = mapOf("Fajr" to 20L, "Isha" to -12L),
        )
    }

    @Test
    fun makkahUmmAlQura() = check(
        "Makkah Umm al-Qura 2026-10-13", 21.4225, 39.8262, "2026-10-13", "Asia/Riyadh",
        CalculationSettings(method = PrayerCalculationMethod.UmmAlQura),
        listOf("04:59", "06:16", "12:07", "15:29", "17:58", "19:28"),
    )

    @Test
    fun newYorkNorthAmerica() = check(
        "New York ISNA 2026-10-13", 40.7128, -74.0060, "2026-10-13", "America/New_York",
        CalculationSettings(method = PrayerCalculationMethod.NorthAmerica),
        listOf("05:50", "07:05", "12:42", "15:50", "18:19", "19:34"),
    )

    @Test
    fun karachiHanafi() = check(
        "Karachi Hanafi 2026-10-13", 24.8607, 67.0011, "2026-10-13", "Asia/Karachi",
        CalculationSettings(method = PrayerCalculationMethod.Karachi, asrMadhab = AsrMadhab.Hanafi),
        listOf("05:13", "06:29", "12:18", "16:30", "18:07", "19:23"),
    )

    private companion object {
        const val TOLERANCE_MINUTES = 2L
    }
}
