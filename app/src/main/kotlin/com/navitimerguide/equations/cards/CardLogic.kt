package com.navitimerguide.equations.cards

import kotlin.math.roundToInt

/**
 * Pure 2-of-3 derive helper.
 *
 * The equation is `a op b = c` where op is multiplication for the
 * multiplication card, and division for the division card. [op] takes the
 * left and right operand and returns the result; [invLeft] reverses for
 * the left operand (i.e. given right & result, return left); [invRight]
 * the same for the right operand.
 *
 * Returns the new (a, b, c) text triple. Null fields stay null. Inputs
 * that fail to parse are treated as null.
 */
internal data class Triple(val a: String, val b: String, val c: String)

internal fun deriveThird(
    a: String,
    b: String,
    c: String,
    lastEdited: Char,                      // 'a', 'b', or 'c'
    op: (Double, Double) -> Double,        // a op b -> c
    invLeft: (Double, Double) -> Double,   // (b, c) -> a
    invRight: (Double, Double) -> Double   // (a, c) -> b
): Triple {
    val ad = a.toDoubleOrNull()
    val bd = b.toDoubleOrNull()
    val cd = c.toDoubleOrNull()
    return when (lastEdited) {
        'a' -> if (ad != null && bd != null) Triple(a, b, fmt(op(ad, bd))) else Triple(a, b, c)
        'b' -> if (ad != null && bd != null) Triple(a, b, fmt(op(ad, bd))) else Triple(a, b, c)
        'c' -> when {
            ad != null && cd != null -> Triple(a, fmt(invRight(ad, cd)), c)
            bd != null && cd != null -> Triple(fmt(invLeft(bd, cd)), b, c)
            else -> Triple(a, b, c)
        }
        else -> Triple(a, b, c)
    }
}

internal fun fmt(value: Double): String {
    if (!value.isFinite()) return ""
    val rounded = value.roundToInt()
    if (kotlin.math.abs(value - rounded) < 1e-6 && kotlin.math.abs(value) < 1e9) {
        return rounded.toString()
    }
    val s = "%.4f".format(value).trimEnd('0').trimEnd('.')
    return s.ifEmpty { "0" }
}
