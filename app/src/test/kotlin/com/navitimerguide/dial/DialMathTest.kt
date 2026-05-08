package com.navitimerguide.dial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DialMathTest {

    private fun closeTo(expected: Double, actual: Double, eps: Double = 1e-6) {
        assertTrue(
            "expected ~$expected, got $actual (Δ=${abs(expected - actual)})",
            abs(expected - actual) <= eps
        )
    }

    @Test fun `valueToAngle 10 is zero`() {
        closeTo(0.0, DialMath.valueToAngle(10.0))
    }

    @Test fun `valueToAngle 100 wraps to zero`() {
        closeTo(0.0, DialMath.valueToAngle(100.0))
    }

    @Test fun `valueToAngle is monotonically increasing across the decade`() {
        var prev = -1.0
        for (v in 10..99) {
            val a = DialMath.valueToAngle(v.toDouble())
            assertTrue("non-monotonic at $v", a > prev)
            prev = a
        }
    }

    @Test fun `round-trip value to angle to value`() {
        for (v in listOf(10.0, 12.5, 16.09, 18.52, 20.0, 33.0, 36.0, 38.0, 50.0, 60.0, 75.0, 99.0)) {
            val r = DialMath.angleToValue(DialMath.valueToAngle(v))
            closeTo(v, r, eps = 1e-9)
        }
    }

    @Test fun `STAT marker is pinned to integer label 40`() {
        closeTo(40.0, DialMath.STAT_MARKER, eps = 1e-9)
    }

    @Test fun `NAUT marker is pinned to integer label 35`() {
        closeTo(35.0, DialMath.NAUT_MARKER, eps = 1e-9)
    }

    @Test fun `STAT-to-KM ratio approximates mile-to-km factor`() {
        // Markers pinned to integer numerals for visual fidelity to the
        // dial; conversion factor accurate to ~0.5 %.
        val ratio = DialMath.KM_MARKER / DialMath.STAT_MARKER
        closeTo(DialMath.MILE_TO_KM, ratio, eps = 0.01)
    }

    @Test fun `NAUT-to-KM ratio approximates nautical-to-km factor`() {
        val ratio = DialMath.KM_MARKER / DialMath.NAUT_MARKER
        closeTo(DialMath.NAUT_TO_KM, ratio, eps = 0.01)
    }

    @Test fun `align rotation places outer X over inner Y`() {
        val rot = DialMath.alignRotation(outerX = 25.0, innerY = 10.0)
        // After this rotation, inner-10 should sit under outer-25.
        val outerOverInner10 = DialMath.outerValueAtInner(10.0, rot)
        closeTo(25.0, outerOverInner10)
    }

    @Test fun `multiplier from rotation matches alignment`() {
        val rot = DialMath.alignRotation(outerX = 35.0, innerY = 10.0)
        // Multiplier should be 3.5
        closeTo(3.5, DialMath.multiplierFromRotation(rot))
    }

    @Test fun `aligning STAT and KM converts miles to km within 1 percent`() {
        // Bezel rotated such that outer-10 sits over inner-STAT_MARKER.
        // Reading at KM should approximate 1 * MILE_TO_KM (within 1 %).
        val rot = DialMath.alignRotation(outerX = 10.0, innerY = DialMath.STAT_MARKER)
        val readKm = DialMath.outerValueAtInner(DialMath.KM_MARKER, rot)
        val readStat = DialMath.outerValueAtInner(DialMath.STAT_MARKER, rot)
        closeTo(DialMath.MILE_TO_KM, readKm / readStat, eps = 0.01)
    }
}
