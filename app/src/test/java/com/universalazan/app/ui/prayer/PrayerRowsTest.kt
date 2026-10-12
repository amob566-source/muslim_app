package com.universalazan.app.ui.prayer

import com.universalazan.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerRowsTest {
    private val rows = notCalculatedPrayerRows()

    @Test
    fun listsPrayersInDailyOrder() {
        val expected = listOf(
            R.string.prayer_fajr,
            R.string.prayer_sunrise,
            R.string.prayer_dhuhr,
            R.string.prayer_asr,
            R.string.prayer_maghrib,
            R.string.prayer_isha,
        )
        assertEquals(expected, rows.map { it.nameRes })
    }

    @Test
    fun exactlyFiveObligatoryPrayers() {
        assertEquals(5, rows.count { it.isObligatory })
    }

    @Test
    fun sunriseIsNotAnObligatoryPrayer() {
        val sunrise = rows.single { it.nameRes == R.string.prayer_sunrise }
        assertFalse(sunrise.isObligatory)
    }

    @Test
    fun noTimesAreInventedBeforeCalculationExists() {
        assertTrue(rows.all { it.time == null })
    }
}
