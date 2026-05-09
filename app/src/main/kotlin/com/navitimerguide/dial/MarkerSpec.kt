package com.navitimerguide.dial

enum class ScaleSide { OUTER, INNER }

enum class MarkerStyle { TRIANGLE_INWARD, TRIANGLE_OUTWARD, TEXT, RED_NUMERAL }

/**
 * One marker (label / triangle) on either scale. [scaleValue] is the
 * scale-value position (10..100). [text] is what to draw if [style] needs it.
 */
data class Marker(
    val scaleValue: Double,
    val side: ScaleSide,
    val style: MarkerStyle,
    val text: String? = null,
    val isRed: Boolean = false
)

object Markers {
    val all: List<Marker> = listOf(
        // Inner fixed scale -------------------------------------------------
        Marker(DialMath.RED_60_MPH, ScaleSide.INNER, MarkerStyle.TRIANGLE_OUTWARD, "MPH", isRed = true),
        Marker(DialMath.RED_10, ScaleSide.INNER, MarkerStyle.RED_NUMERAL, "10", isRed = true),
        Marker(DialMath.RED_36, ScaleSide.INNER, MarkerStyle.RED_NUMERAL, "36", isRed = true),
        Marker(DialMath.KM_MARKER, ScaleSide.INNER, MarkerStyle.TEXT, "KM", isRed = true),
        // STAT/NAUT labels include the numeral so the chapter ring reads
        // "STAT. 40" / "NAUT. 35" as a single phrase, exactly like the photo.
        Marker(DialMath.STAT_MARKER, ScaleSide.INNER, MarkerStyle.TRIANGLE_OUTWARD, "STAT. 40", isRed = true),
        Marker(DialMath.NAUT_MARKER, ScaleSide.INNER, MarkerStyle.TRIANGLE_OUTWARD, "NAUT. 35", isRed = true),

        // Outer rotating scale ---------------------------------------------
        Marker(DialMath.RED_60_MPH, ScaleSide.OUTER, MarkerStyle.TRIANGLE_INWARD, null, isRed = true),
        Marker(DialMath.RED_10, ScaleSide.OUTER, MarkerStyle.RED_NUMERAL, "10", isRed = true),
        Marker(DialMath.RED_36, ScaleSide.OUTER, MarkerStyle.RED_NUMERAL, "36", isRed = true)
    )
}
