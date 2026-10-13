package com.universalazan.app.domain.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class PrayerTimeCalculatorTest {
    private val london = 51.5074 to -0.1278
    private val londonZone = ZoneId.of("Europe/London")
    private val october13 = LocalDate.of(2026, 10, 13)

    private fun calc(
        lat: Double,
        lon: Double,
        date: LocalDate,
        zone: ZoneId,
        settings: CalculationSettings = CalculationSettings(),
    ): DailyPrayerTimes {
        val result = PrayerTimeCalculator.calculate(lat, lon, date, zone, settings)
        assertTrue("expected success, got $result", result is PrayerCalculationResult.Success)
        return (result as PrayerCalculationResult.Success).times
    }

    private fun DailyPrayerTimes.inOrder() = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)

    private fun assertOrdered(times: DailyPrayerTimes) {
        val list = times.inOrder()
        list.zipWithNext().forEach { (a, b) -> assertTrue("$a should be before $b", a.isBefore(b)) }
    }

    @Test
    fun invalidCoordinatesAreRejected() {
        listOf(91.0 to 0.0, -91.0 to 0.0, 0.0 to 181.0, 0.0 to -181.0, Double.NaN to 0.0).forEach { (lat, lon) ->
            assertEquals(
                PrayerCalculationResult.InvalidCoordinates,
                PrayerTimeCalculator.calculate(lat, lon, october13, londonZone),
            )
        }
    }

    @Test
    fun everyMethodGivesTimesInTheRightOrder() {
        PrayerCalculationMethod.entries.forEach { method ->
            val times = calc(london.first, london.second, october13, londonZone, CalculationSettings(method = method))
            assertOrdered(times)
        }
    }

    @Test
    fun hanafiAsrIsLaterThanStandard() {
        val standard = calc(london.first, london.second, october13, londonZone)
        val hanafi = calc(london.first, london.second, october13, londonZone, CalculationSettings(asrMadhab = AsrMadhab.Hanafi))
        assertTrue(Duration.between(standard.asr, hanafi.asr).toMinutes() > 30)
        // Nothing else changes.
        assertEquals(standard.fajr, hanafi.fajr)
        assertEquals(standard.dhuhr, hanafi.dhuhr)
        assertEquals(standard.maghrib, hanafi.maghrib)
        assertEquals(standard.isha, hanafi.isha)
    }

    @Test
    fun methodChangesFajrAndIsha() {
        val mwl = calc(london.first, london.second, october13, londonZone)
        val isna = calc(london.first, london.second, october13, londonZone, CalculationSettings(method = PrayerCalculationMethod.NorthAmerica))
        // ISNA uses 15° instead of 18°/17°, so Fajr is later and Isha earlier.
        assertTrue(isna.fajr.isAfter(mwl.fajr))
        assertTrue(isna.isha.isBefore(mwl.isha))
    }

    @Test
    fun perPrayerAdjustmentsShiftEachTimeExactly() {
        val base = calc(london.first, london.second, october13, londonZone)
        val adjusted = calc(
            london.first, london.second, october13, londonZone,
            CalculationSettings(adjustments = PrayerAdjustments(fajr = 5, sunrise = -2, dhuhr = 3, asr = 0, maghrib = 2, isha = -10)),
        )
        assertEquals(5, Duration.between(base.fajr, adjusted.fajr).toMinutes())
        assertEquals(-2, Duration.between(base.sunrise, adjusted.sunrise).toMinutes())
        assertEquals(3, Duration.between(base.dhuhr, adjusted.dhuhr).toMinutes())
        assertEquals(0, Duration.between(base.asr, adjusted.asr).toMinutes())
        assertEquals(2, Duration.between(base.maghrib, adjusted.maghrib).toMinutes())
        assertEquals(-10, Duration.between(base.isha, adjusted.isha).toMinutes())
    }

    @Test(expected = IllegalArgumentException::class)
    fun adjustmentsOutsideRangeAreRefused() {
        PrayerAdjustments(fajr = 61)
    }

    @Test
    fun highLatitudeRulesChangeSummerFajrAndIshaInLondon() {
        val june21 = LocalDate.of(2026, 6, 21)
        fun withRule(rule: HighLatitudeOption) =
            calc(london.first, london.second, june21, londonZone, CalculationSettings(highLatitude = rule))
        val middle = withRule(HighLatitudeOption.MiddleOfTheNight)
        val seventh = withRule(HighLatitudeOption.SeventhOfTheNight)
        val twilight = withRule(HighLatitudeOption.TwilightAngle)
        val automatic = withRule(HighLatitudeOption.Automatic)
        listOf(middle, seventh, twilight, automatic).forEach(::assertOrdered)
        assertNotEquals(middle.isha, seventh.isha)
        assertNotEquals(middle.fajr, seventh.fajr)
        // Above 48° the library's automatic choice is one-seventh of the night.
        assertEquals(seventh, automatic)
        // Dhuhr, Asr and Maghrib do not depend on the high-latitude rule.
        assertEquals(middle.dhuhr, seventh.dhuhr)
        assertEquals(middle.maghrib, twilight.maghrib)
    }

    @Test
    fun polarDayAndNightCannotBeCalculated() {
        val tromso = 69.6489 to 18.9551
        val oslo = ZoneId.of("Europe/Oslo")
        listOf(LocalDate.of(2026, 6, 21), LocalDate.of(2026, 12, 21)).forEach { date ->
            assertEquals(
                "Tromsø $date",
                PrayerCalculationResult.SunDoesNotRiseOrSet,
                PrayerTimeCalculator.calculate(tromso.first, tromso.second, date, oslo),
            )
        }
        // In October the sun rises and sets normally there.
        assertOrdered(calc(tromso.first, tromso.second, october13, oslo))
    }

    @Test
    fun consecutiveDaysGiveDifferentTimes() {
        val today = calc(london.first, london.second, october13, londonZone)
        val tomorrow = calc(london.first, london.second, october13.plusDays(1), londonZone)
        assertEquals(october13.plusDays(1), tomorrow.dhuhr.toLocalDate())
        // In October, days get shorter: sunrise later, maghrib earlier.
        assertTrue(tomorrow.sunrise.toLocalTime().isAfter(today.sunrise.toLocalTime()))
        assertTrue(tomorrow.maghrib.toLocalTime().isBefore(today.maghrib.toLocalTime()))
    }

    @Test
    fun dhuhrFallsOnTheRequestedLocalDateEverywhere() {
        // Includes UTC+14, UTC-11 and half/quarter-hour zones.
        val places = listOf(
            Triple(-36.8485, 174.7635, "Pacific/Auckland"),
            Triple(21.3069, -157.8583, "Pacific/Honolulu"),
            Triple(27.7172, 85.3240, "Asia/Kathmandu"),
            Triple(28.6139, 77.2090, "Asia/Kolkata"),
            Triple(1.8721, -157.4278, "Pacific/Kiritimati"),
            Triple(-14.2756, -170.7020, "Pacific/Pago_Pago"),
            Triple(-33.8688, 151.2093, "Australia/Sydney"),
        )
        listOf(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 7, 15)).forEach { date ->
            places.forEach { (lat, lon, zoneId) ->
                val times = calc(lat, lon, date, ZoneId.of(zoneId))
                assertEquals("$zoneId $date", date, times.dhuhr.toLocalDate())
                assertTrue("$zoneId dhuhr ${times.dhuhr}", times.dhuhr.hour in 11..13)
                assertOrdered(times)
            }
        }
    }

    @Test
    fun resultsDoNotDependOnThePhonesTimeZone() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val inUtc = calc(-36.8485, 174.7635, october13, ZoneId.of("Pacific/Auckland"))
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            val inHonolulu = calc(-36.8485, 174.7635, october13, ZoneId.of("Pacific/Auckland"))
            assertEquals(inUtc, inHonolulu)
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun timesAreExpressedInTheRequestedZone() {
        val tokyo = calc(35.6762, 139.6503, october13, ZoneId.of("Asia/Tokyo"))
        assertEquals(ZoneId.of("Asia/Tokyo"), tokyo.dhuhr.zone)
        assertEquals(9 * 3600, tokyo.dhuhr.offset.totalSeconds)
    }
}
