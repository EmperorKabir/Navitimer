package com.navitimerguide.viewmodel

import androidx.lifecycle.ViewModel
import com.navitimerguide.dial.DialMath
import com.navitimerguide.equations.EquationEngine
import com.navitimerguide.equations.EquationGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow

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

    fun equationsFor(angle: Double): List<EquationGroup> = EquationEngine.compute(angle)
}
