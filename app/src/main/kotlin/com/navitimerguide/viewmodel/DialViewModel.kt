package com.navitimerguide.viewmodel

import androidx.lifecycle.ViewModel
import com.navitimerguide.dial.DialMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DialViewModel : ViewModel() {

    // Start aligned outer-25 to inner-10, so the equations panel demonstrates
    // a "× 2.5" multiplier and the sample worked examples are non-trivial on
    // first launch. Aligning to 0° would show only identity equations.
    private val _rotationDegrees = MutableStateFlow(DialMath.alignRotation(outerX = 25.0, innerY = 10.0))
    val rotationDegrees: StateFlow<Double> = _rotationDegrees.asStateFlow()

    fun rotateBy(deltaDegrees: Double) {
        _rotationDegrees.value = DialMath.wrap360(_rotationDegrees.value + deltaDegrees)
    }

    fun setRotation(angle: Double) {
        _rotationDegrees.value = DialMath.wrap360(angle)
    }

    /** Snap the bezel so outer X sits above inner Y. */
    fun snapAlign(outerX: Double, innerY: Double) {
        if (outerX <= 0 || innerY <= 0) return
        _rotationDegrees.value = DialMath.alignRotation(outerX, innerY)
    }

    fun reset() {
        _rotationDegrees.value = 0.0
    }

    /** Set the bezel so that outer-10 sits over inner-(k*10), giving multiplier k. */
    fun setMultiplier(k: Double) {
        if (k <= 0 || !k.isFinite()) return
        setRotation(DialMath.alignRotation(outerX = 10.0 * k, innerY = 10.0))
    }

    fun currentMultiplier(): Double = DialMath.multiplierFromRotation(_rotationDegrees.value)

    /** Convenience: align distance (outer) with time-in-minutes (inner). */
    fun setSpeedTimeDistance(distance: Double, timeMinutes: Double) {
        if (distance > 0 && timeMinutes > 0) snapAlign(outerX = distance, innerY = timeMinutes)
    }
}
