package com.universalazan.app.audio

import com.universalazan.app.R
import com.universalazan.app.domain.prayer.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AzanSoundTest {
    @Test
    fun fullAzanUsesFajrVersionOnlyForFajr() {
        assertEquals(R.raw.azan_full_fajr, azanRawResource(Prayer.Fajr, AzanSound.Full))
        listOf(Prayer.Dhuhr, Prayer.Asr, Prayer.Maghrib, Prayer.Isha).forEach {
            assertEquals("$it", R.raw.azan_full, azanRawResource(it, AzanSound.Full))
        }
    }

    @Test
    fun shortAzanIsTheSameForEveryPrayer() {
        Prayer.entries.forEach { assertEquals("$it", R.raw.azan_short, azanRawResource(it, AzanSound.Short)) }
    }

    @Test
    fun silentPlaysNothing() {
        Prayer.entries.forEach { assertNull("$it", azanRawResource(it, AzanSound.Silent)) }
    }

    @Test
    fun defaultsAreFullAzanForEveryPrayer() {
        val defaults = AzanSoundSettings()
        Prayer.entries.forEach { assertEquals(AzanSound.Full, defaults.soundFor(it)) }
    }

    @Test
    fun eachPrayerCanHaveItsOwnSound() {
        // The example from the project brief.
        val settings = AzanSoundSettings()
            .with(Prayer.Fajr, AzanSound.Full)
            .with(Prayer.Dhuhr, AzanSound.Short)
            .with(Prayer.Asr, AzanSound.Silent)
        assertEquals(AzanSound.Full, settings.soundFor(Prayer.Fajr))
        assertEquals(AzanSound.Short, settings.soundFor(Prayer.Dhuhr))
        assertEquals(AzanSound.Silent, settings.soundFor(Prayer.Asr))
        assertEquals(AzanSound.Full, settings.soundFor(Prayer.Maghrib))
        assertEquals(AzanSound.Full, settings.soundFor(Prayer.Isha))
    }
}
