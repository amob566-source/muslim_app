package com.universalazan.app.domain.qibla

import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Qibla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs

class QiblaCalculatorTest {

    private fun bearing(lat: Double, lon: Double): Double {
        val result = QiblaCalculator.calculate(lat, lon)
        if (result !is QiblaResult.Direction) fail("($lat, $lon): expected a direction, got $result")
        return (result as QiblaResult.Direction).bearingDegrees
    }

    /** Smallest difference between two angles, e.g. 359° and 1° differ by 2°. */
    private fun angleDiff(a: Double, b: Double): Double {
        val d = abs(a - b) % 360.0
        return if (d > 180) 360 - d else d
    }

    private fun assertBearing(label: String, expected: Double, actual: Double, tolerance: Double) {
        assertTrue("$label: expected $expected°, got $actual°", angleDiff(expected, actual) <= tolerance)
    }

    /**
     * Independent reference: AlAdhan Qibla API (api.aladhan.com/v1/qibla), fetched 13 Oct 2026.
     * Includes places either side of the international date line (Fiji, Samoa) and far north.
     */
    @Test
    fun matchesIndependentReferenceBearings() {
        val references = listOf(
            Triple("London", 51.5074 to -0.1278, 118.98724),
            Triple("New York", 40.7128 to -74.0060, 58.48171),
            Triple("Kuala Lumpur", 3.1390 to 101.6869, 292.53772),
            Triple("Jakarta", -6.2088 to 106.8456, 295.15175),
            Triple("Sydney", -33.8688 to 151.2093, 277.49959),
            Triple("Tokyo", 35.6762 to 139.6503, 292.99871),
            Triple("Cairo", 30.0444 to 31.2357, 136.13734),
            Triple("Cape Town", -33.9249 to 18.4241, 23.35250),
            Triple("Suva, Fiji", -18.1416 to 178.4419, 281.89275),
            Triple("Apia, Samoa", -13.8333 to -171.7667, 288.69843),
            Triple("Honolulu", 21.3069 to -157.8583, 336.88645),
            Triple("Anchorage", 61.2181 to -149.9003, 350.88309),
            Triple("Reykjavik", 64.1466 to -21.9426, 106.11766),
            Triple("Madinah", 24.4672 to 39.6024, 176.08408),
            Triple("Sao Paulo", -23.5505 to -46.6333, 68.93934),
            Triple("Lima", -12.0464 to -77.0428, 72.02655),
        )
        references.forEach { (name, coords, expected) ->
            // AlAdhan uses Kaaba 21.4225241, 39.8261818; ours is 21.4225, 39.8262, hence 0.01°.
            assertBearing(name, expected, bearing(coords.first, coords.second), tolerance = 0.01)
        }
    }

    /** Official: JAKIM (Malaysia) publishes 292° 31' 16" for zone WLY01 (Kuala Lumpur). */
    @Test
    fun matchesJakimOfficialBearingForKualaLumpur() {
        val jakim = 292.0 + 31.0 / 60 + 16.0 / 3600
        // JAKIM's zone reference point is not published with the bearing, so allow 0.1°.
        assertBearing("JAKIM WLY01", jakim, bearing(3.1390, 101.6869), tolerance = 0.1)
    }

    /** Second implementation: the Adhan library's own Qibla formula (different equation). */
    @Test
    fun agreesWithAdhanLibraryWorldwide() {
        var checked = 0
        for (lat in -85..85 step 5) {
            for (lon in -180..180 step 10) {
                val ours = QiblaCalculator.calculate(lat.toDouble(), lon.toDouble())
                if (ours !is QiblaResult.Direction) continue
                val adhan = Qibla(Coordinates(lat.toDouble(), lon.toDouble())).direction
                assertBearing("($lat, $lon)", adhan, ours.bearingDegrees, tolerance = 0.01)
                checked++
            }
        }
        assertTrue(checked > 1000)
    }

    @Test
    fun bearingIsAlwaysInHalfOpenRange() {
        for (lat in -90..90 step 3) {
            for (lon in -180..180 step 3) {
                val result = QiblaCalculator.calculate(lat.toDouble(), lon.toDouble())
                if (result is QiblaResult.Direction) {
                    assertTrue("($lat, $lon) -> ${result.bearingDegrees}", result.bearingDegrees >= 0.0 && result.bearingDegrees < 360.0)
                }
            }
        }
    }

    @Test
    fun dateLineGivesContinuousResults() {
        // 179.9° E and 179.9° W are only ~22 km apart; their bearings must be nearly equal.
        val east = bearing(-18.0, 179.9)
        val west = bearing(-18.0, -179.9)
        assertTrue("east $east, west $west", angleDiff(east, west) < 0.2)
        // +180 and -180 are the same meridian.
        assertBearing("±180", bearing(10.0, 180.0), bearing(10.0, -180.0), tolerance = 1e-9)
    }

    @Test
    fun cardinalSanityChecks() {
        // Due north of the Kaaba (same longitude): Qibla is due south.
        assertBearing("north of Kaaba", 180.0, bearing(40.0, 39.8262), tolerance = 1e-6)
        // Due south of the Kaaba: Qibla is due north.
        assertBearing("south of Kaaba", 0.0, bearing(-10.0, 39.8262), tolerance = 1e-6)
    }

    @Test
    fun polesGiveAValidDirection() {
        // At the poles every direction is south (north); the formula still returns a valid angle.
        assertTrue(bearing(90.0, 0.0) in 0.0..359.999)
        assertTrue(bearing(-90.0, 0.0) in 0.0..359.999)
    }

    @Test
    fun distanceToMakkah() {
        val london = QiblaCalculator.calculate(51.5074, -0.1278) as QiblaResult.Direction
        // Great-circle distance London-Makkah is about 4,790 km.
        assertEquals(4790.0, london.distanceKm, 15.0)
    }

    @Test
    fun specialPoints() {
        assertEquals(QiblaResult.AtKaaba, QiblaCalculator.calculate(QiblaCalculator.KAABA_LATITUDE, QiblaCalculator.KAABA_LONGITUDE))
        assertEquals(QiblaResult.Antipode, QiblaCalculator.calculate(-QiblaCalculator.KAABA_LATITUDE, QiblaCalculator.KAABA_LONGITUDE - 180.0))
        // A few hundred metres from the Kaaba there is still a clear direction.
        assertTrue(QiblaCalculator.calculate(21.4260, 39.8262) is QiblaResult.Direction)
    }

    @Test
    fun invalidCoordinatesAreRejected() {
        listOf(91.0 to 0.0, -90.5 to 0.0, 0.0 to 180.1, 0.0 to -200.0, Double.NaN to 0.0, 0.0 to Double.NaN).forEach { (lat, lon) ->
            assertEquals("($lat, $lon)", QiblaResult.InvalidCoordinates, QiblaCalculator.calculate(lat, lon))
        }
    }

    @Test
    fun normalizeDegreesKeepsRangeHalfOpen() {
        assertEquals(350.0, normalizeDegrees(-10.0), 1e-9)
        assertEquals(0.0, normalizeDegrees(360.0), 0.0)
        assertEquals(0.0, normalizeDegrees(720.0), 0.0)
        assertEquals(10.0, normalizeDegrees(370.0), 1e-9)
        assertEquals(0.0, normalizeDegrees(-1e-15), 0.0) // -1e-15 + 360 rounds to 360 -> 0
        assertTrue(normalizeDegrees(-1e-10) < 360.0)
    }

    @Test
    fun compassPointNames() {
        assertEquals("N", compassPoint(0.0))
        assertEquals("N", compassPoint(359.0))
        assertEquals("NNE", compassPoint(11.25))
        assertEquals("ESE", compassPoint(118.99))
        assertEquals("S", compassPoint(180.0))
        assertEquals("W", compassPoint(270.0))
        assertEquals("WNW", compassPoint(292.54))
    }
}
