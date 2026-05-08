package com.navitimerguide.viewmodel

import androidx.lifecycle.ViewModel
import com.navitimerguide.dial.DialMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DialViewModel : ViewModel() {

    // Bezel rotation, 0 = outer-60 aligned with inner-60 at top of dial
    // (matches the real Navitimer's default position).
    private val _rotationDegrees = MutableStateFlow(0.0)
    val rotationDegrees: StateFlow<Double> = _rotationDegrees.asStateFlow()

    // Two free-form input fields the user types into. They drive the bezel
    // alignment on commit. Defaults match the dial's default position so
    // first-launch shows a clean state.
    private val _outerInput = MutableStateFlow("60")
    val outerInput: StateFlow<String> = _outerInput.asStateFlow()

    private val _innerInput = MutableStateFlow("60")
    val innerInput: StateFlow<String> = _innerInput.asStateFlow()

    fun setOuterText(s: String) { _outerInput.value = s }
    fun setInnerText(s: String) { _innerInput.value = s }

    /** Snap the bezel to align outer-X with inner-Y, using the typed inputs. */
    fun commitInputs() {
        val x = _outerInput.value.toDoubleOrNull() ?: return
        val y = _innerInput.value.toDoubleOrNull() ?: return
        if (x > 0 && y > 0) snapAlign(x, y)
    }

    fun rotateBy(deltaDegrees: Double) {
        _rotationDegrees.value = DialMath.wrap360(_rotationDegrees.value + deltaDegrees)
    }

    fun setRotation(angle: Double) {
        _rotationDegrees.value = DialMath.wrap360(angle)
    }

    fun snapAlign(outerX: Double, innerY: Double) {
        if (outerX <= 0 || innerY <= 0) return
        _rotationDegrees.value = DialMath.alignRotation(outerX, innerY)
    }

    fun reset() {
        _rotationDegrees.value = 0.0
        _outerInput.value = "60"
        _innerInput.value = "60"
    }

    fun setMultiplier(k: Double) {
        if (k <= 0 || !k.isFinite()) return
        setRotation(DialMath.alignRotation(outerX = 10.0 * k, innerY = 10.0))
    }

    fun currentMultiplier(): Double = DialMath.multiplierFromRotation(_rotationDegrees.value)
}
