package com.navitimerguide.equations

/**
 * One row to render in the equations panel. [text] is the human-readable
 * formula (e.g. "20 / 8 = 2.50"). [value] is the numeric result for tests.
 * [highlight] when true, the row is the bezel-derived live answer; when
 * false it is a derivative / worked example.
 */
data class EquationRow(
    val text: String,
    val value: Double,
    val highlight: Boolean = false
)

data class EquationGroup(
    val title: String,
    val description: String,
    val rows: List<EquationRow>
)
