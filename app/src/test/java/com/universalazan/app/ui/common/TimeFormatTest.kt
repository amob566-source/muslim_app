package com.universalazan.app.ui.common

import com.universalazan.app.domain.prayer.AdjustableTime
import com.universalazan.app.domain.prayer.PrayerAdjustments
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

class TimeFormatTest {
    @Test
    fun twentyFourHour() {
        assertEquals("05:30", formatClockTime(LocalTime.of(5, 30), use24Hour = true, locale = Locale.UK))
        assertEquals("17:05", formatClockTime(LocalTime.of(17, 5), use24Hour = true, locale = Locale.UK))
    }

    @Test
    fun twelveHour() {
        assertEquals("5:30 AM", formatClockTime(LocalTime.of(5, 30), use24Hour = false, locale = Locale.US))
        assertEquals("5:05 PM", formatClockTime(LocalTime.of(17, 5), use24Hour = false, locale = Locale.US))
        assertEquals("12:00 PM", formatClockTime(LocalTime.NOON, use24Hour = false, locale = Locale.US))
    }

    @Test
    fun adjustmentsChangeOnlyOneTimeAndStayInRange() {
        val start = PrayerAdjustments()
        val fajrPlusTen = (1..10).fold(start) { acc, _ -> acc.changedBy(AdjustableTime.Fajr, 1) }
        assertEquals(10, fajrPlusTen.fajr)
        assertEquals(0, fajrPlusTen.isha)
        assertEquals(PrayerAdjustments.MAX_MINUTES, start.changedBy(AdjustableTime.Asr, 500).asr)
        assertEquals(PrayerAdjustments.MIN_MINUTES, start.changedBy(AdjustableTime.Maghrib, -500).maghrib)
        assertEquals(-3, start.changedBy(AdjustableTime.Sunrise, -3).minutesFor(AdjustableTime.Sunrise))
    }
}
