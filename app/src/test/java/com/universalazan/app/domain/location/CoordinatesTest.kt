package com.universalazan.app.domain.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinatesTest {
    private fun lat(text: String) = parseCoordinate(text, CoordinateAxis.Latitude)
    private fun lon(text: String) = parseCoordinate(text, CoordinateAxis.Longitude)

    @Test
    fun acceptsOrdinaryDecimals() {
        assertEquals(CoordinateParseResult.Valid(51.5074), lat("51.5074"))
        assertEquals(CoordinateParseResult.Valid(-0.1278), lon("-0.1278"))
        assertEquals(CoordinateParseResult.Valid(12.0), lat("+12"))
        assertEquals(CoordinateParseResult.Valid(0.5), lat(".5"))
        assertEquals(CoordinateParseResult.Valid(21.4225), lat("  21.4225  "))
    }

    @Test
    fun acceptsCommaAsDecimalSeparator() {
        assertEquals(CoordinateParseResult.Valid(51.5074), lat("51,5074"))
    }

    @Test
    fun acceptsUnicodeMinusSign() {
        assertEquals(CoordinateParseResult.Valid(-33.8688), lat("−33.8688"))
    }

    @Test
    fun acceptsExactBoundaries() {
        assertEquals(CoordinateParseResult.Valid(90.0), lat("90"))
        assertEquals(CoordinateParseResult.Valid(-90.0), lat("-90"))
        assertEquals(CoordinateParseResult.Valid(180.0), lon("180"))
        assertEquals(CoordinateParseResult.Valid(-180.0), lon("-180"))
    }

    @Test
    fun rejectsOutOfRange() {
        assertEquals(CoordinateParseResult.OutOfRange, lat("90.0001"))
        assertEquals(CoordinateParseResult.OutOfRange, lat("-91"))
        assertEquals(CoordinateParseResult.OutOfRange, lon("180.5"))
        assertEquals(CoordinateParseResult.OutOfRange, lon("-200"))
    }

    @Test
    fun rejectsEmptyAndNonNumbers() {
        assertEquals(CoordinateParseResult.Empty, lat(""))
        assertEquals(CoordinateParseResult.Empty, lat("   "))
        listOf("abc", "NaN", "Infinity", "1e2", "12d", "1.2.3", "--5", "51,5,1", "51°", "0x10").forEach {
            assertEquals("input '$it'", CoordinateParseResult.NotANumber, lat(it))
        }
    }

    @Test
    fun validityChecks() {
        assertTrue(isValidLatitude(0.0))
        assertFalse(isValidLatitude(Double.NaN))
        assertFalse(isValidLatitude(90.1))
        assertTrue(isValidLongitude(-180.0))
        assertFalse(isValidLongitude(181.0))
    }

    @Test
    fun formatsWithHemispheres() {
        assertEquals("51.5074° N, 0.1278° W", formatCoordinates(51.5074, -0.1278))
        assertEquals("33.8688° S, 151.2093° E", formatCoordinates(-33.8688, 151.2093))
    }
}
