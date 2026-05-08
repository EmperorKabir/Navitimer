package com.navitimerguide.equations

import com.navitimerguide.dial.DialMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class EquationEngineTest {

    @Test fun `identity rotation has 1x multiplier`() {
        val groups = EquationEngine.compute(0.0)
        val division = groups.first { it.title == "Division" }
        // The first row is the constant ratio; at zero rotation it should equal 1.0
        val ratio = division.rows.first().value
        assertTrue("ratio at 0° should be 1, got $ratio", abs(ratio - 1.0) < 1e-9)
    }

    @Test fun `rotation set for 25-on-10 yields x2_5 multiplier`() {
        val rot = DialMath.alignRotation(outerX = 25.0, innerY = 10.0)
        val mult = EquationEngine.compute(rot)
            .first { it.title == "Multiplication" }
            .rows.first().value
        assertTrue("expected 2.5, got $mult", abs(mult - 2.5) < 1e-9)
    }

    @Test fun `4 hours mapping reads 240 minutes and 14400 seconds`() {
        // The spreadsheet worked example: align outer 40 with inner 10 -> 4 hours.
        val rot = DialMath.alignRotation(outerX = 40.0, innerY = 10.0)
        val time = EquationEngine.compute(rot)
            .first { it.title == "Time units (60 / 36)" }
        // hours row = 4
        val hours = time.rows[0].value
        val minutes = time.rows[1].value
        val seconds = time.rows[2].value
        assertTrue("hours expected 4, got $hours", abs(hours - 4.0) < 1e-9)
        assertTrue("minutes expected 240, got $minutes", abs(minutes - 240.0) < 1e-9)
        assertTrue("seconds expected 14400, got $seconds", abs(seconds - 14400.0) < 1e-9)
    }

    @Test fun `every group is non-empty`() {
        val groups = EquationEngine.compute(0.0)
        assertTrue(groups.size >= 6)
        groups.forEach { g ->
            assertTrue("group ${g.title} has no rows", g.rows.isNotEmpty())
        }
    }
}
