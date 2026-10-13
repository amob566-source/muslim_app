package com.universalazan.app.alarms

import com.universalazan.app.audio.AzanSound
import com.universalazan.app.audio.AzanSoundSettings
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.domain.prayer.CalculationSettings
import com.universalazan.app.domain.prayer.HighLatitudeOption
import com.universalazan.app.domain.prayer.Prayer
import com.universalazan.app.domain.prayer.PrayerCalculationResult
import com.universalazan.app.domain.prayer.PrayerTimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PrayerAlarmPlannerTest {
    private val london = SavedLocation("London", 51.5074, -0.1278, "Europe/London", LocationSource.City)
    private val calc = CalculationSettings()
    private val allFull = AzanSoundSettings()

    private fun plan(
        location: SavedLocation? = london,
        sounds: AzanSoundSettings = allFull,
        now: Instant,
        settings: CalculationSettings = calc,
    ) = PrayerAlarmPlanner.plan(location, settings, sounds, now)

    @Test
    fun afternoonPlansRestOfTodayThenTomorrowMorning() {
        // 13 Oct 2026, 14:00 London (BST).
        val alarms = plan(now = Instant.parse("2026-10-13T13:00:00Z"))
        assertEquals(
            listOf(Prayer.Asr, Prayer.Maghrib, Prayer.Isha, Prayer.Fajr, Prayer.Dhuhr),
            alarms.map { it.prayer },
        )
        val today = LocalDate.of(2026, 10, 13)
        assertEquals(today, alarms.first { it.prayer == Prayer.Asr }.date)
        assertEquals(today.plusDays(1), alarms.first { it.prayer == Prayer.Fajr }.date)
        assertEquals(today.plusDays(1), alarms.first { it.prayer == Prayer.Dhuhr }.date)
    }

    @Test
    fun alarmTimesMatchTheCalculatedPrayerTimes() {
        val now = Instant.parse("2026-10-13T13:00:00Z")
        val times = (PrayerTimeCalculator.calculate(london.latitude, london.longitude, LocalDate.of(2026, 10, 13), london.zoneId!!, calc)
            as PrayerCalculationResult.Success).times
        val asr = plan(now = now).first { it.prayer == Prayer.Asr }
        assertEquals(times.asr.toInstant(), asr.time)
    }

    @Test
    fun oneAlarmPerPrayerAndAllInTheFuture() {
        val now = Instant.parse("2026-10-13T13:00:00Z")
        val alarms = plan(now = now)
        assertEquals(alarms.size, alarms.map { it.prayer }.toSet().size)
        assertTrue(alarms.all { it.time.isAfter(now) })
        assertEquals(alarms.sortedBy { it.time }, alarms)
    }

    @Test
    fun alarmIsTheEarliestFutureOccurrenceEverywhere() {
        // Brute force check across places, seasons and times of day.
        val places = listOf(
            london,
            SavedLocation("Oslo", 59.9139, 10.7522, "Europe/Oslo", LocationSource.City),
            SavedLocation("Auckland", -36.8485, 174.7635, "Pacific/Auckland", LocationSource.City),
            SavedLocation("Kiritimati", 1.8721, -157.4278, "Pacific/Kiritimati", LocationSource.City),
            SavedLocation("Makkah", 21.4266, 39.8256, "Asia/Riyadh", LocationSource.City),
        )
        val instants = listOf("2026-06-21T00:30:00Z", "2026-06-21T11:00:00Z", "2026-12-21T23:59:00Z", "2026-03-29T00:59:00Z")
            .map(Instant::parse)
        val settings = calc.copy(highLatitude = HighLatitudeOption.MiddleOfTheNight)
        places.forEach { place ->
            instants.forEach { now ->
                val alarms = plan(location = place, now = now, settings = settings)
                val zone = place.zoneId!!
                val candidates = (-2L..4L).mapNotNull { offset ->
                    (PrayerTimeCalculator.calculate(place.latitude, place.longitude, now.atZone(zone).toLocalDate().plusDays(offset), zone, settings)
                        as? PrayerCalculationResult.Success)?.times
                }
                Prayer.entries.forEach { prayer ->
                    val expected = candidates.map { it.timeOf(prayer).toInstant() }.filter { it.isAfter(now) }.minOrNull()
                    assertEquals("${place.name} $now $prayer", expected, alarms.firstOrNull { it.prayer == prayer }?.time)
                }
            }
        }
    }

    @Test
    fun silentPrayersGetNoAlarm() {
        val sounds = allFull.with(Prayer.Asr, AzanSound.Silent).with(Prayer.Isha, AzanSound.Silent)
        val alarms = plan(sounds = sounds, now = Instant.parse("2026-10-13T13:00:00Z"))
        assertEquals(setOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Maghrib), alarms.map { it.prayer }.toSet())
    }

    @Test
    fun soundChoiceIsCarriedIntoTheAlarm() {
        val sounds = allFull.with(Prayer.Dhuhr, AzanSound.Short)
        val alarms = plan(sounds = sounds, now = Instant.parse("2026-10-13T13:00:00Z"))
        assertEquals(AzanSound.Short, alarms.first { it.prayer == Prayer.Dhuhr }.sound)
        assertEquals(AzanSound.Full, alarms.first { it.prayer == Prayer.Asr }.sound)
    }

    @Test
    fun noLocationOrUnknownTimeZoneMeansNoAlarms() {
        val now = Instant.parse("2026-10-13T13:00:00Z")
        assertTrue(plan(location = null, now = now).isEmpty())
        assertTrue(plan(location = london.copy(timeZoneId = "Nowhere/Land"), now = now).isEmpty())
    }

    @Test
    fun polarNightMeansNoAlarmsRatherThanWrongOnes() {
        val tromso = SavedLocation("Tromsø", 69.6489, 18.9551, "Europe/Oslo", LocationSource.City)
        assertTrue(plan(location = tromso, now = Instant.parse("2026-12-21T12:00:00Z")).isEmpty())
    }

    @Test
    fun daylightSavingNightUsesTheNewClockTime() {
        // 24 Oct 2026 22:00 BST; clocks go back at 02:00 on 25 Oct. Next Fajr is 25 Oct in GMT.
        val alarms = plan(now = Instant.parse("2026-10-24T21:00:00Z"))
        val fajr = alarms.first { it.prayer == Prayer.Fajr }
        assertEquals(LocalDate.of(2026, 10, 25), fajr.date)
        val local = fajr.time.atZone(ZoneId.of("Europe/London"))
        assertEquals(0, local.offset.totalSeconds) // GMT
    }

    @Test
    fun justAfterAPrayerTheNextOneIsTomorrows() {
        val now = Instant.parse("2026-10-13T13:00:00Z")
        val asr = plan(now = now).first { it.prayer == Prayer.Asr }
        // One second after Asr: Asr moves to tomorrow (this is how a fired alarm re-arms).
        val after = plan(now = asr.time.plusSeconds(1)).first { it.prayer == Prayer.Asr }
        assertEquals(asr.date.plusDays(1), after.date)
    }

    @Test
    fun missedAlarmRule() {
        val due = Instant.parse("2026-10-13T12:48:00Z")
        assertFalse(PrayerAlarmPlanner.isMissed(due, due))
        assertFalse(PrayerAlarmPlanner.isMissed(due, due.plus(Duration.ofMinutes(4))))
        assertFalse(PrayerAlarmPlanner.isMissed(due, due.plus(Duration.ofMinutes(5))))
        assertTrue(PrayerAlarmPlanner.isMissed(due, due.plus(Duration.ofMinutes(6))))
        assertTrue(PrayerAlarmPlanner.isMissed(due, due.plus(Duration.ofHours(3))))
    }

    @Test
    fun dailyCheckIsNextHalfPastThreeLocalTime() {
        val london = ZoneId.of("Europe/London")
        // 13 Oct 14:00 BST -> 14 Oct 03:30 BST.
        assertEquals(Instant.parse("2026-10-14T02:30:00Z"), PrayerAlarmPlanner.nextDailyCheck(Instant.parse("2026-10-13T13:00:00Z"), london))
        // 13 Oct 01:00 BST -> same day 03:30 BST.
        assertEquals(Instant.parse("2026-10-13T02:30:00Z"), PrayerAlarmPlanner.nextDailyCheck(Instant.parse("2026-10-13T00:00:00Z"), london))
        // Exactly at 03:30 -> the next day (always strictly in the future).
        assertEquals(Instant.parse("2026-10-14T02:30:00Z"), PrayerAlarmPlanner.nextDailyCheck(Instant.parse("2026-10-13T02:30:00Z"), london))
    }

    @Test
    fun dailyCheckOnTheNightClocksGoBack() {
        // 24 Oct 22:00 BST; clocks go back at 02:00 on 25 Oct, so 03:30 that night is GMT.
        val check = PrayerAlarmPlanner.nextDailyCheck(Instant.parse("2026-10-24T21:00:00Z"), ZoneId.of("Europe/London"))
        assertEquals(Instant.parse("2026-10-25T03:30:00Z"), check)
    }
}
