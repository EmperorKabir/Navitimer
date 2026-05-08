package com.navitimerguide.equations

import com.navitimerguide.dial.DialMath
import kotlin.math.roundToInt

/**
 * Pure transformation: bezel rotation -> grouped, live equation rows.
 *
 * Every formula is derived deterministically from the rotation angle alone,
 * so unit tests can pin known angles and assert exact results.
 *
 * Each group corresponds to a section of the source-of-truth spreadsheet
 * (rows 10..33) plus the standard E6B "extras".
 */
object EquationEngine {

    private val sampleOuter = listOf(15.0, 20.0, 30.0, 50.0, 75.0)
    private val sampleInner = listOf(12.0, 25.0, 40.0, 60.0)

    fun compute(rotationDegrees: Double): List<EquationGroup> {
        val multiplier = DialMath.multiplierFromRotation(rotationDegrees)
        return listOf(
            divisionGroup(rotationDegrees, multiplier),
            multiplicationGroup(rotationDegrees, multiplier),
            speedTimeDistanceGroup(rotationDegrees),
            statKmGroup(rotationDegrees),
            nautKmGroup(rotationDegrees),
            timeUnitsGroup(rotationDegrees),
            extrasGroup(rotationDegrees, multiplier)
        )
    }

    // ------------------------------------------------------------------
    // 1. Division (spreadsheet rows 10–11)
    //
    // "outer / inner_aligned = outer_at_inner_10"
    // i.e. for any outer X aligned with inner Y, X / Y is constant and
    // equals the outer value sitting above inner-10.
    // ------------------------------------------------------------------
    private fun divisionGroup(rot: Double, multiplier: Double): EquationGroup {
        val rows = mutableListOf<EquationRow>()
        rows += EquationRow(
            text = "Constant ratio (outer ÷ inner) = ${fmt(multiplier)}",
            value = multiplier,
            highlight = true
        )
        for (outer in sampleOuter) {
            val inner = DialMath.innerValueAtOuter(outer, rot)
            val q = outer / inner
            rows += EquationRow(
                text = "${fmt(outer)} ÷ ${fmt(inner)} = ${fmt(q)}",
                value = q
            )
        }
        return EquationGroup(
            title = "Division",
            description = "Align outer X with inner Y; every outer ÷ inner pair shares the same ratio. Read the answer above inner 10.",
            rows = rows
        )
    }

    // ------------------------------------------------------------------
    // 2. Multiplication (spreadsheet rows 13–17)
    //
    // Align outer-10 to inner-K  →  any outer X gives inner = X * K.
    // (Equivalently, multiplier set by current bezel position.)
    // ------------------------------------------------------------------
    private fun multiplicationGroup(rot: Double, multiplier: Double): EquationGroup {
        // K = outerValueAtInner(10) / 10 = multiplierFromRotation(rot).
        // This is the canonical "outer ÷ inner" ratio (mod decade) the slide
        // rule encodes — and matches the spreadsheet convention of reading
        // the outer value above the inner unit index.
        val k = multiplier
        val rows = mutableListOf<EquationRow>()
        rows += EquationRow(
            text = "Multiplier (× ${fmt(k)}) — outer reading above inner 10",
            value = k,
            highlight = true
        )
        for (inner in sampleInner) {
            val product = inner * k
            rows += EquationRow(
                text = "${fmt(inner)} × ${fmt(k)} = ${fmt(product)}",
                value = product
            )
        }
        return EquationGroup(
            title = "Multiplication",
            description = "Set the multiplier by aligning outer 10 with the desired multiplier on the inner scale. Then for any inner X, the value above on the outer is X × multiplier.",
            rows = rows
        )
    }

    // ------------------------------------------------------------------
    // 3. Speed / Time / Distance (spreadsheet rows 19–22)
    //
    // The MPH index is at inner-60 (12 o'clock). When you align distance
    // (outer) with time-in-minutes (inner), the value above MPH = speed
    // in mph. We surface three worked time/distance pairs.
    // ------------------------------------------------------------------
    private fun speedTimeDistanceGroup(rot: Double): EquationGroup {
        val mph = DialMath.outerValueAtInner(DialMath.RED_60_MPH, rot)
        val rows = mutableListOf<EquationRow>()
        rows += EquationRow(
            text = "Read above MPH (inner 60) → ${fmt(mph)} mph",
            value = mph,
            highlight = true
        )
        for (timeMin in listOf(10.0, 15.0, 30.0, 45.0, 60.0)) {
            val distance = DialMath.outerValueAtInner(timeMin, rot)
            // Verify: distance/timeMin * 60 = mph (within rotation invariant)
            val derived = distance / timeMin * 60.0
            rows += EquationRow(
                text = "Distance ${fmt(distance)} in ${fmt(timeMin)} min → ${fmt(derived)} mph",
                value = derived
            )
        }
        return EquationGroup(
            title = "Speed / Time / Distance",
            description = "Align distance (outer) with time-in-minutes (inner). Read mph above the MPH index (inner 60).",
            rows = rows
        )
    }

    // ------------------------------------------------------------------
    // 4. Statute miles ↔ km (spreadsheet row 24)
    //
    // STAT marker on inner = KM_MARKER / 1.609344. Aligning outer X with
    // STAT marker reads X * 1.609344 above the KM marker on the same outer
    // scale (because KM/STAT = 1.609344 by construction).
    // ------------------------------------------------------------------
    private fun statKmGroup(rot: Double): EquationGroup {
        val rows = mutableListOf<EquationRow>()
        // Live read at the markers
        val outerAtSTAT = DialMath.outerValueAtInner(DialMath.STAT_MARKER, rot)
        val outerAtKM = DialMath.outerValueAtInner(DialMath.KM_MARKER, rot)
        rows += EquationRow(
            text = "Outer above STAT marker → ${fmt(outerAtSTAT)} statute miles",
            value = outerAtSTAT,
            highlight = true
        )
        rows += EquationRow(
            text = "Outer above KM marker  → ${fmt(outerAtKM)} kilometres",
            value = outerAtKM,
            highlight = true
        )
        // Direct calculator (independent of bezel)
        for (mi in listOf(1.0, 5.0, 10.0, 60.0, 100.0)) {
            val km = mi * DialMath.MILE_TO_KM
            rows += EquationRow(
                text = "${fmt(mi)} mi = ${fmt(km)} km",
                value = km
            )
        }
        return EquationGroup(
            title = "Statute miles ↔ Kilometres",
            description = "Align statute miles on the outer scale with the STAT marker on the inner; read kilometres above the KM marker.",
            rows = rows
        )
    }

    // ------------------------------------------------------------------
    // 5. Nautical miles ↔ km (spreadsheet row 25)
    // ------------------------------------------------------------------
    private fun nautKmGroup(rot: Double): EquationGroup {
        val rows = mutableListOf<EquationRow>()
        val outerAtNAUT = DialMath.outerValueAtInner(DialMath.NAUT_MARKER, rot)
        val outerAtKM = DialMath.outerValueAtInner(DialMath.KM_MARKER, rot)
        rows += EquationRow(
            text = "Outer above NAUT marker → ${fmt(outerAtNAUT)} nautical miles",
            value = outerAtNAUT,
            highlight = true
        )
        rows += EquationRow(
            text = "Outer above KM marker  → ${fmt(outerAtKM)} kilometres",
            value = outerAtKM,
            highlight = true
        )
        for (nm in listOf(1.0, 5.0, 10.0, 60.0, 100.0)) {
            val km = nm * DialMath.NAUT_TO_KM
            rows += EquationRow(
                text = "${fmt(nm)} nm = ${fmt(km)} km",
                value = km
            )
        }
        return EquationGroup(
            title = "Nautical miles ↔ Kilometres",
            description = "Same as STAT but using the NAUT marker (1 nm = 1.852 km).",
            rows = rows
        )
    }

    // ------------------------------------------------------------------
    // 6. Time units via 60 / 36 (spreadsheet rows 27–33)
    //
    // Worked spreadsheet example:  "4 hours" → align outer 40 with inner 10
    //   → above inner 60: 24 (= 240 minutes)
    //   → above inner 36: 14.4 (= 14,400 seconds)
    // We surface this live for whatever hours value the user has chosen.
    // ------------------------------------------------------------------
    private fun timeUnitsGroup(rot: Double): EquationGroup {
        val hours = DialMath.outerValueAtInner(10.0, rot) / 10.0   // multiplier
        val minutes = hours * 60.0
        val seconds = hours * 3600.0
        val rows = mutableListOf<EquationRow>()
        rows += EquationRow(
            text = "Hours set (multiplier on inner 10) = ${fmt(hours)} h",
            value = hours,
            highlight = true
        )
        rows += EquationRow(
            text = "Above inner 60: ${fmt(DialMath.outerValueAtInner(60.0, rot))} → ${fmt(minutes)} minutes",
            value = minutes,
            highlight = true
        )
        rows += EquationRow(
            text = "Above inner 36: ${fmt(DialMath.outerValueAtInner(36.0, rot))} → ${fmt(seconds)} seconds",
            value = seconds,
            highlight = true
        )
        rows += EquationRow(
            text = "Identity: 60 × 60 = 3600 (red 36 marker = 3600 seconds in an hour)",
            value = 3600.0
        )
        return EquationGroup(
            title = "Time units (60 / 36)",
            description = "Align the hours value to outer-10 → reads minutes above inner 60 and seconds above inner 36.",
            rows = rows
        )
    }

    // ------------------------------------------------------------------
    // 7. Extras (E6B-style derivative uses)
    // ------------------------------------------------------------------
    private fun extrasGroup(rot: Double, multiplier: Double): EquationGroup {
        val rows = mutableListOf<EquationRow>()
        rows += EquationRow(
            text = "Fuel burn: at ${fmt(multiplier)} gph, ${fmt(60.0 * multiplier)} gallons / hour",
            value = 60.0 * multiplier
        )
        rows += EquationRow(
            text = "Currency: rate × any amount on outer = converted on inner (× ${fmt(1.0 / multiplier)})",
            value = 1.0 / multiplier
        )
        rows += EquationRow(
            text = "Percentage: tip ${fmt(multiplier * 100)}%  →  on a 50 bill = ${fmt(50.0 * multiplier)}",
            value = 50.0 * multiplier
        )
        return EquationGroup(
            title = "Extras (fuel, currency, %)",
            description = "Once a multiplier is set on the bezel, the same alignment encodes any rate-style problem.",
            rows = rows
        )
    }

    private fun fmt(value: Double): String {
        if (!value.isFinite()) return "—"
        // Human-readable: integer if close, else 2 dp
        val rounded = value.roundToInt()
        if (kotlin.math.abs(value - rounded) < 1e-9 && kotlin.math.abs(value) < 1e9) {
            return rounded.toString()
        }
        return "%.2f".format(value)
    }
}
