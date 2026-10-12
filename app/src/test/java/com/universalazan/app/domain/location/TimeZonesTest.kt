package com.universalazan.app.domain.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TimeZonesTest {
    @Test
    fun recognisesValidAndInvalidIds() {
        assertEquals(ZoneId.of("Europe/London"), zoneIdOrNull("Europe/London"))
        assertNull(zoneIdOrNull("Mars/Olympus_Mons"))
        assertNull(zoneIdOrNull(""))
        assertNull(zoneIdOrNull(null))
    }

    @Test
    fun choicesContainRegionZonesOnly() {
        val choices = timeZoneChoices()
        assertTrue("Europe/London" in choices)
        assertTrue("Asia/Karachi" in choices)
        assertTrue("America/New_York" in choices)
        assertTrue(choices.none { it.startsWith("Etc/") || it.startsWith("SystemV/") || '/' !in it })
        assertEquals(choices.sorted(), choices)
    }

    @Test
    fun londonOffsetFollowsDaylightSaving() {
        val london = ZoneId.of("Europe/London")
        assertEquals("UTC+00:00", utcOffsetLabel(london, Instant.parse("2026-01-15T12:00:00Z")))
        assertEquals("UTC+01:00", utcOffsetLabel(london, Instant.parse("2026-07-15T12:00:00Z")))
    }

    @Test
    fun formatsNegativeAndHalfHourOffsets() {
        val winter = Instant.parse("2026-01-15T12:00:00Z")
        assertEquals("UTC-05:00", utcOffsetLabel(ZoneId.of("America/New_York"), winter))
        assertEquals("UTC+05:30", utcOffsetLabel(ZoneId.of("Asia/Kolkata"), winter))
    }
}
