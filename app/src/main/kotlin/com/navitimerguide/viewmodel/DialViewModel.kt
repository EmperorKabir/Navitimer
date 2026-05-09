package com.navitimerguide.viewmodel

import androidx.lifecycle.ViewModel
import com.navitimerguide.dial.DialMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.abs
import kotlin.math.round

enum class ChronoState { IDLE, RUNNING, STOPPED }

class DialViewModel : ViewModel() {

    // ---------------------------------------------------------------- bezel

    private val _rotationDegrees = MutableStateFlow(0.0)
    val rotationDegrees: StateFlow<Double> = _rotationDegrees.asStateFlow()

    private val _outerInput = MutableStateFlow("60")
    val outerInput: StateFlow<String> = _outerInput.asStateFlow()

    private val _innerInput = MutableStateFlow("60")
    val innerInput: StateFlow<String> = _innerInput.asStateFlow()

    fun rotateBy(deltaDegrees: Double) {
        _rotationDegrees.value = DialMath.wrap360(_rotationDegrees.value + deltaDegrees)
        syncOuterFromRotation()
    }

    fun setRotation(angle: Double) {
        _rotationDegrees.value = DialMath.wrap360(angle)
        syncOuterFromRotation()
    }

    fun snapAlign(outerX: Double, innerY: Double) {
        if (outerX <= 0 || innerY <= 0) return
        _rotationDegrees.value = DialMath.alignRotation(outerX, innerY)
        syncOuterFromRotation()
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

    // ------------------------------------------------------------- inputs
    //
    // Both fields are user-typed, independent strings. Neither is silently
    // overwritten while the user is typing. The bezel only updates on
    // commit (focus-out / IME Done), and the OUTER live readout only
    // refreshes when the bezel itself rotates (drag, preset, reset).
    //
    // Nonsense input (empty, multiple dots, etc.) fails parseDouble in
    // [commitInputs] and is silently ignored — the bezel does nothing
    // and the field keeps the user's typed text.

    fun setOuterText(s: String) {
        _outerInput.value = s
    }

    fun setInnerText(s: String) {
        _innerInput.value = s
    }

    fun commitInputs() {
        val x = _outerInput.value.toDoubleOrNull() ?: return
        val y = _innerInput.value.toDoubleOrNull() ?: return
        if (x <= 0 || y <= 0 || !x.isFinite() || !y.isFinite()) return
        snapAlign(x, y)
    }

    private fun syncOuterFromRotation() {
        val innerY = _innerInput.value.toDoubleOrNull() ?: return
        if (innerY <= 0) return
        val outerX = DialMath.outerValueAtInner(innerY, _rotationDegrees.value)
        _outerInput.value = formatNum(outerX)
    }

    private fun formatNum(v: Double): String {
        if (!v.isFinite()) return ""
        val rounded2 = round(v * 100.0) / 100.0
        if (abs(rounded2 - rounded2.toInt()) < 1e-9 && abs(rounded2) < 1e9) {
            return rounded2.toInt().toString()
        }
        return "%.2f".format(rounded2).trimEnd('0').trimEnd('.')
    }

    // ----------------------------------------------------------- chronograph

    private val _chronoState = MutableStateFlow(ChronoState.IDLE)
    val chronoState: StateFlow<ChronoState> = _chronoState.asStateFlow()

    private val _accumulatedMs = MutableStateFlow(0L)
    private var startInstant: Instant? = null

    fun chronoStartStop() {
        when (_chronoState.value) {
            ChronoState.IDLE, ChronoState.STOPPED -> {
                startInstant = Clock.System.now()
                _chronoState.value = ChronoState.RUNNING
            }
            ChronoState.RUNNING -> {
                val now = Clock.System.now()
                val delta = (now - (startInstant ?: now)).inWholeMilliseconds
                _accumulatedMs.value = _accumulatedMs.value + delta
                startInstant = null
                _chronoState.value = ChronoState.STOPPED
            }
        }
    }

    fun chronoReset() {
        if (_chronoState.value == ChronoState.RUNNING) return
        _accumulatedMs.value = 0L
        startInstant = null
        _chronoState.value = ChronoState.IDLE
    }

    fun currentChronoMs(): Long {
        val acc = _accumulatedMs.value
        return when (_chronoState.value) {
            ChronoState.RUNNING -> {
                val now = Clock.System.now()
                acc + (now - (startInstant ?: now)).inWholeMilliseconds
            }
            else -> acc
        }
    }
}
