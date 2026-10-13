package com.universalazan.app.ui.prayer

import com.universalazan.app.R
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.domain.prayer.AsrMadhab
import com.universalazan.app.domain.prayer.CalculationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PrayerTimesStateTest {
    private val london = SavedLocation("London", 51.5074, -0.1278, "Europe/London", LocationSource.City)
    private val settings = CalculationSettings()

    // 13 Oct 2026, 14:00 in London (BST): after Dhuhr (12:47), before Asr (15:35).
    private val londonAfternoon = Instant.parse("2026-10-13T13:00:00Z")

    private fun nextName(state: PrayerTimesUiState.Ready) = state.prayers.singleOrNull { it.isNext }?.nameRes

    @Test
    fun noLocationShowsNoTimes() {
        val state = PrayerTimesViewModel.buildState(null, settings, londonAfternoon)
        assertEquals(PrayerProblem.NoLocation, state.problem)
        assertTrue(state.prayers.all { it.time == null })
    }

    @Test
    fun calculatesTimesForSavedLocation() {
        val state = PrayerTimesViewModel.buildState(london, settings, londonAfternoon)
        assertNull(state.problem)
        assertEquals(LocalDate.of(2026, 10, 13), state.date)
        assertEquals(6, state.prayers.size)
        assertTrue(state.prayers.all { it.time != null })
        assertEquals(R.string.prayer_asr, nextName(state))
    }

    @Test
    fun nextPrayerMovesOnAndSunriseIsNeverNext() {
        // 06:00 BST: after Fajr (05:30), before Sunrise (07:21). Sunrise is skipped, so next is Dhuhr.
        val earlyMorning = Instant.parse("2026-10-13T05:00:00Z")
        assertEquals(R.string.prayer_dhuhr, nextName(PrayerTimesViewModel.buildState(london, settings, earlyMorning)))
        // 03:00 BST: before Fajr.
        val night = Instant.parse("2026-10-13T02:00:00Z")
        assertEquals(R.string.prayer_fajr, nextName(PrayerTimesViewModel.buildState(london, settings, night)))
        // 23:00 BST: after Isha; tomorrow's Fajr is not on this list, so nothing is marked.
        val lateEvening = Instant.parse("2026-10-13T22:00:00Z")
        assertNull(nextName(PrayerTimesViewModel.buildState(london, settings, lateEvening)))
    }

    @Test
    fun todayIsTheDateAtTheLocationNotOnThePhone() {
        val auckland = SavedLocation("Auckland", -36.8485, 174.7635, "Pacific/Auckland", LocationSource.City)
        // 13 Oct 12:00 UTC is already 14 Oct (01:00) in Auckland.
        val state = PrayerTimesViewModel.buildState(auckland, settings, Instant.parse("2026-10-13T12:00:00Z"))
        assertEquals(LocalDate.of(2026, 10, 14), state.date)
        assertEquals(LocalDate.of(2026, 10, 14), state.prayers.first { it.nameRes == R.string.prayer_dhuhr }.time!!.toLocalDate())
    }

    @Test
    fun dateRollsOverAtLocalMidnight() {
        val before = PrayerTimesViewModel.buildState(london, settings, Instant.parse("2026-10-13T22:59:00Z"))
        val after = PrayerTimesViewModel.buildState(london, settings, Instant.parse("2026-10-13T23:01:00Z"))
        assertEquals(LocalDate.of(2026, 10, 13), before.date)
        assertEquals(LocalDate.of(2026, 10, 14), after.date)
        assertTrue(before.prayers[2].time != after.prayers[2].time)
    }

    @Test
    fun settingsChangeTheResult() {
        val standard = PrayerTimesViewModel.buildState(london, settings, londonAfternoon)
        val hanafi = PrayerTimesViewModel.buildState(london, settings.copy(asrMadhab = AsrMadhab.Hanafi), londonAfternoon)
        assertTrue(hanafi.prayers[3].time!!.isAfter(standard.prayers[3].time))
    }

    @Test
    fun unknownTimeZoneIsReportedInsteadOfGuessed() {
        val state = PrayerTimesViewModel.buildState(london.copy(timeZoneId = "Nowhere/Land"), settings, londonAfternoon)
        assertEquals(PrayerProblem.InvalidTimeZone, state.problem)
        assertTrue(state.prayers.all { it.time == null })
    }

    @Test
    fun polarNightIsReported() {
        val tromso = SavedLocation("Tromsø", 69.6489, 18.9551, "Europe/Oslo", LocationSource.City)
        val state = PrayerTimesViewModel.buildState(tromso, settings, Instant.parse("2026-12-21T12:00:00Z"))
        assertEquals(PrayerProblem.SunDoesNotRiseOrSet, state.problem)
        assertTrue(state.prayers.all { it.time == null })
    }
}
