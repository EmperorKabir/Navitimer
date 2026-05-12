package com.navitimerguide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.navitimerguide.controls.CurvedPresets
import com.navitimerguide.dial.WatchDial
import com.navitimerguide.dial.bezelDragRotation
import com.navitimerguide.equations.BezelInputs
import com.navitimerguide.equations.ConverterInputs
import com.navitimerguide.equations.FloatingEquations
import com.navitimerguide.viewmodel.DialViewModel

@Composable
fun NavitimerApp() {
    val vm: DialViewModel = viewModel()
    val rotation by vm.rotationDegrees.collectAsStateWithLifecycle()
    val outerText by vm.outerInput.collectAsStateWithLifecycle()
    val innerText by vm.innerInput.collectAsStateWithLifecycle()
    val statText by vm.statInput.collectAsStateWithLifecycle()
    val nautText by vm.nautInput.collectAsStateWithLifecycle()
    val kmText by vm.kmInput.collectAsStateWithLifecycle()
    val chronoState by vm.chronoState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isWide = maxWidth >= 720.dp
            if (isWide) {
                Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    DialColumn(
                        modifier = Modifier.weight(1f),
                        rotation = rotation,
                        chronoState = chronoState,
                        chronoMillisProvider = vm::currentChronoMs,
                        outerText = outerText,
                        innerText = innerText,
                        statText = statText,
                        nautText = nautText,
                        kmText = kmText,
                        vm = vm
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FloatingEquations(
                            rotationDegrees = rotation,
                            outer = outerText,
                            inner = innerText,
                            statRead = statText,
                            nautRead = nautText,
                            kmRead = kmText
                        )
                    }
                }
            } else {
                // Compact / portrait: dial + buttons stay anchored at the
                // top; the live equations panel is its own bounded scroll
                // area at the bottom so the user can scroll the equations
                // independently without losing sight of the watch.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DialColumn(
                        modifier = Modifier.fillMaxWidth(),
                        rotation = rotation,
                        chronoState = chronoState,
                        chronoMillisProvider = vm::currentChronoMs,
                        outerText = outerText,
                        innerText = innerText,
                        statText = statText,
                        nautText = nautText,
                        kmText = kmText,
                        vm = vm
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        FloatingEquations(
                            rotationDegrees = rotation,
                            outer = outerText,
                            inner = innerText,
                            statRead = statText,
                            nautRead = nautText,
                            kmRead = kmText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialColumn(
    modifier: Modifier,
    rotation: Double,
    chronoState: com.navitimerguide.viewmodel.ChronoState,
    chronoMillisProvider: () -> Long,
    outerText: String,
    innerText: String,
    statText: String,
    nautText: String,
    kmText: String,
    vm: DialViewModel
) {
    val haptics = LocalHapticFeedback.current

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Reset (left, on its own) + Examples arc (right) above the watch.
        CurvedPresets(
            onSetAngle = vm::setRotation,
            onReset = vm::reset,
            // Tall enough to fit the steeper chip arc (centre chip up
            // top, outer chips ~44 dp lower) plus the EXAMPLES caption.
            modifier = Modifier.fillMaxWidth().height(96.dp)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val side = maxWidth
            val rOuter = side.value * 0.5f * 0.88f

            // Both floating panels report their actual measured size back
            // via onSizeChanged so the overlap calc adapts to whatever
            // the bumped fonts / number of rows actually produce on this
            // device. No more hard-coded width / height constants.
            var bezelSize by remember { mutableStateOf(IntSize.Zero) }
            var converterSize by remember { mutableStateOf(IntSize.Zero) }
            val density = LocalDensity.current
            val bezelDp = with(density) {
                bezelSize.width.toDp().value to bezelSize.height.toDp().value
            }
            val converterDp = with(density) {
                converterSize.width.toDp().value to converterSize.height.toDp().value
            }
            fun overlapFor(w: Float, h: Float): Float {
                if (w == 0f || h == 0f) return 0f
                val dx = side.value / 2f - w
                val dy = side.value / 2f - h
                val cornerDist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                return (rOuter - cornerDist).coerceAtLeast(0f)
            }
            val overlap = maxOf(
                overlapFor(bezelDp.first, bezelDp.second),
                overlapFor(converterDp.first, converterDp.second)
            )
            // On Fold 7 (~720 dp wide) both overlaps resolve to 0 and the
            // layout is identical to a plain square BoxWithConstraints.
            // On narrower screens, the container grows by however many dp
            // it takes to clear whichever box is the tighter fit.
            val containerHeight = side + overlap.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(containerHeight)
            ) {
                // Watch square — anchored to TOP-CENTER. Only the watch
                // itself receives the bezel drag gesture; the empty
                // overflow strip below (if any) ignores drags.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .align(Alignment.TopCenter)
                        .bezelDragRotation { vm.rotateBy(it) }
                ) {
                    WatchDial(
                        bezelRotationDegrees = rotation,
                        chronoState = chronoState,
                        chronoMillisProvider = chronoMillisProvider,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Top pusher tap target (start / stop)
                    Box(modifier = Modifier
                        .offset(x = side * 0.85f, y = side * 0.20f)
                        .size(width = side * 0.13f, height = side * 0.13f)
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.chronoStartStop()
                        })
                    // Bottom pusher tap target (reset)
                    Box(modifier = Modifier
                        .offset(x = side * 0.85f, y = side * 0.67f)
                        .size(width = side * 0.13f, height = side * 0.13f)
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            vm.chronoReset()
                        })
                }

                // Floating boxes anchored to the container's bottom
                // corners. If overlap == 0 they sit inside the watch
                // square's corners; if overlap > 0 the container has been
                // extended downward, so the boxes sit BELOW the dial.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 2.dp, bottom = 2.dp)
                        .onSizeChanged { bezelSize = it }
                ) {
                    BezelInputs(
                        outer = outerText,
                        inner = innerText,
                        onOuterChange = vm::setOuterText,
                        onInnerChange = vm::setInnerText,
                        onCommit = vm::commitInputs
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 2.dp, bottom = 2.dp)
                        .onSizeChanged { converterSize = it }
                ) {
                    ConverterInputs(
                        stat = statText,
                        naut = nautText,
                        km = kmText,
                        onStatChange = vm::setStatText,
                        onNautChange = vm::setNautText,
                        onKmChange = vm::setKmText,
                        onCommitStat = vm::commitStat,
                        onCommitNaut = vm::commitNaut,
                        onCommitKm = vm::commitKm
                    )
                }
            }
        }
    }
}
