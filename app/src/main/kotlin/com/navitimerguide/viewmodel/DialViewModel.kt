package com.navitimerguide.viewmodel

import androidx.lifecycle.ViewModel
import com.navitimerguide.dial.DialMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

enum class ChronoState { IDLE, RUNNING, STOPPED }

class DialViewModel : ViewModel() {

    // ----- bezel rotation -------------------------------------------------
    private val _rotationDegrees = MutableStateFlow(0.0)
    val rotationDegrees: StateFlow<Double> = _rotationDegrees.asStateFlow()

    private val _outerInput = MutableStateFlow("60")
    val outerInput: StateFlow<String> = _outerInput.asStateFlow()

    private val _innerInput = MutableStateFlow("60")
    val innerInput: StateFlow<String> = _innerInput.asStateFlow()

    fun setOuterText(s: String) { _outerInput.value = s }
    fun setInnerText(s: String) { _innerInput.value = s }

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

    // ----- chronograph ----------------------------------------------------
    //
    // Top pusher (start/stop): toggles between RUNNING and STOPPED. From IDLE
    // it starts running. From RUNNING it stops, freezing the elapsed time.
    // From STOPPED it resumes.
    //
    // Bottom pusher (reset): only valid when NOT running. Zeroes the chrono
    // and returns to IDLE.

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

    /** Total elapsed milliseconds at this instant, including time since the latest start. */
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
