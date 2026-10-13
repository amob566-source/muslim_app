package com.universalazan.app.sensors

import com.universalazan.app.ui.qibla.QiblaViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class HeadingMathTest {

    private fun angleDiff(a: Double, b: Double): Double {
        val d = abs(a - b) % 360.0
        return if (d > 180) 360 - d else d
    }

    private fun assertHeading(expected: Double, actual: Double?, label: String) {
        assertNotNull("$label: no heading", actual)
        assertTrue("$label: expected $expected, got $actual", angleDiff(expected, actual!!) < 0.01)
    }

    /** Builds Android's device-to-world matrix from the device's X, Y, Z axes in (east, north, up). */
    private fun matrix(x: DoubleArray, y: DoubleArray, z: DoubleArray) = floatArrayOf(
        x[0].toFloat(), y[0].toFloat(), z[0].toFloat(),
        x[1].toFloat(), y[1].toFloat(), z[1].toFloat(),
        x[2].toFloat(), y[2].toFloat(), z[2].toFloat(),
    )

    /**
     * Portrait phone whose screen top / camera faces compass direction [heading] (degrees),
     * tilted up by [pitch] degrees (0 = lying flat, 90 = held upright facing the user).
     */
    private fun portrait(heading: Double, pitch: Double): FloatArray {
        val h = Math.toRadians(heading)
        val p = Math.toRadians(pitch)
        val forward = doubleArrayOf(sin(h), cos(h), 0.0)
        val up = doubleArrayOf(0.0, 0.0, 1.0)
        val right = doubleArrayOf(cos(h), -sin(h), 0.0)
        val y = DoubleArray(3) { cos(p) * forward[it] + sin(p) * up[it] }
        val z = DoubleArray(3) { -sin(p) * forward[it] + cos(p) * up[it] }
        return matrix(right, y, z)
    }

    @Test
    fun flatPhoneFacingEachDirection() {
        for (h in 0 until 360 step 15) {
            assertHeading(h.toDouble(), HeadingMath.heading(portrait(h.toDouble(), 0.0), 0), "flat $h")
        }
    }

    @Test
    fun uprightPhoneUsesCameraDirection() {
        for (h in 0 until 360 step 15) {
            assertHeading(h.toDouble(), HeadingMath.heading(portrait(h.toDouble(), 90.0), 0), "upright $h")
        }
    }

    @Test
    fun tiltedPhoneGivesTheSameHeading() {
        for (pitch in listOf(10.0, 30.0, 45.0, 60.0, 80.0)) {
            assertHeading(123.0, HeadingMath.heading(portrait(123.0, pitch), 0), "pitch $pitch")
        }
    }

    @Test
    fun identityMatrixPointsNorth() {
        // Android: identity = device flat, top pointing to north.
        val identity = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        assertHeading(0.0, HeadingMath.heading(identity, 0), "identity")
    }

    @Test
    fun landscapeUsesTheCorrectScreenEdge() {
        // Phone lying flat. In each display rotation, a different device edge is the top of the screen.
        // Build the device so that the edge which is "top of screen" points to 70°.
        val h = Math.toRadians(70.0)
        val toward = doubleArrayOf(sin(h), cos(h), 0.0)
        val left = doubleArrayOf(-cos(h), sin(h), 0.0)
        val right = doubleArrayOf(cos(h), -sin(h), 0.0)
        val away = doubleArrayOf(-sin(h), -cos(h), 0.0)
        val up = doubleArrayOf(0.0, 0.0, 1.0)
        // ROTATION_90: screen top is device +X, so device Y points to the screen's left.
        assertHeading(70.0, HeadingMath.heading(matrix(toward, left, up), 1), "rotation 90")
        // ROTATION_180: screen top is device -Y.
        assertHeading(70.0, HeadingMath.heading(matrix(left, away, up), 2), "rotation 180")
        // ROTATION_270: screen top is device -X.
        assertHeading(70.0, HeadingMath.heading(matrix(away, right, up), 3), "rotation 270")
    }

    @Test
    fun signedTurnChoosesTheShortWay() {
        assertEquals(29.0, HeadingMath.signedTurn(90.0, 119.0), 1e-9)
        assertEquals(-31.0, HeadingMath.signedTurn(150.0, 119.0), 1e-9)
        assertEquals(20.0, HeadingMath.signedTurn(350.0, 10.0), 1e-9)
        assertEquals(-20.0, HeadingMath.signedTurn(10.0, 350.0), 1e-9)
        assertEquals(180.0, abs(HeadingMath.signedTurn(0.0, 180.0)), 1e-9)
    }

    @Test
    fun facingWithinTolerance() {
        assertTrue(HeadingMath.isFacing(116.0, 119.0))
        assertTrue(HeadingMath.isFacing(358.0, 2.0))
        assertFalse(HeadingMath.isFacing(110.0, 119.0))
    }

    @Test
    fun magneticInterferenceThresholds() {
        assertFalse(HeadingMath.isMagneticInterference(48f)) // typical in Europe
        assertFalse(HeadingMath.isMagneticInterference(25f))
        assertTrue(HeadingMath.isMagneticInterference(150f)) // magnet nearby
        assertTrue(HeadingMath.isMagneticInterference(5f))
    }

    @Test
    fun smootherHandlesWrapAround() {
        val smoother = AngleSmoother(alpha = 0.5)
        smoother.add(350.0)
        val result = smoother.add(10.0)
        // Average of 350° and 10° is 0°, not 180°.
        assertTrue("got $result", angleDiff(0.0, result) < 0.5)
    }

    @Test
    fun smootherFollowsANewDirection() {
        val smoother = AngleSmoother(alpha = 0.2)
        smoother.add(0.0)
        var value = 0.0
        repeat(60) { value = smoother.add(90.0) }
        assertTrue("got $value", angleDiff(90.0, value) < 0.5)
    }

    @Test
    fun liveStateAddsDeclinationForTrueNorth() {
        // Magnetic heading 100°, declination +3° east => true heading 103°.
        val east = QiblaViewModel.liveState(CompassSample(100.0, CompassAccuracy.High, 48f), declination = 3.0)
        assertEquals(103.0, east.trueHeading, 1e-9)
        assertFalse(east.interference)
        // West declination and wrap-around: 1° magnetic with -5° => 356° true.
        val west = QiblaViewModel.liveState(CompassSample(1.0, CompassAccuracy.Low, 200f), declination = -5.0)
        assertEquals(356.0, west.trueHeading, 1e-9)
        assertTrue(west.interference)
    }
}
