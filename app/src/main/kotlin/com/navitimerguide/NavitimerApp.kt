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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Constraints
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
            // Hoist the equations-panel scroll state so it survives any
            // recomposition triggered by font-scale / rotation changes.
            val equationsScroll = rememberScrollState()
            if (isWide) {
                Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    DialColumn(
                        modifier = Modifier.weight(1f),
                        isWide = true,
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
                    // Right column: input panels on top (their own Row),
                    // live equations below in a scrollable sub-column.
                    // The dial column on the left stays full-size at any
                    // font scale because its sizing no longer depends
                    // on the input panels' heights.
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            BezelInputs(
                                outer = outerText,
                                inner = innerText,
                                onOuterChange = vm::setOuterText,
                                onInnerChange = vm::setInnerText,
                                onCommit = vm::commitInputs
                            )
                            Spacer(modifier = Modifier.weight(1f))
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
                        Spacer(Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(equationsScroll),
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
                        isWide = false,
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
    isWide: Boolean,
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
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Reset (left, on its own) + Examples arc (right) above the watch.
        CurvedPresets(
            onSetAngle = vm::setRotation,
            onReset = vm::reset,
            // Tall enough to fit the steeper chip arc (centre chip up
            // top, outer chips ~44 dp lower) plus the EXAMPLES caption.
            modifier = Modifier.fillMaxWidth().height(96.dp)
        )

        val bezelInputs: @Composable () -> Unit = {
            BezelInputs(
                outer = outerText,
                inner = innerText,
                onOuterChange = vm::setOuterText,
                onInnerChange = vm::setInnerText,
                onCommit = vm::commitInputs
            )
        }
        val converterInputs: @Composable () -> Unit = {
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

        if (isWide) {
            // Tablet / wide canvas: dial fills the dial column width at
            // its full square aspect ratio. The input panels are NOT
            // drawn in this column — they have been moved to the top of
            // the equations column (sibling Row in the parent) so the
            // dial's size never depends on input heights and never
            // shrinks at any font scale.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val side = maxWidth
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .bezelDragRotation { vm.rotateBy(it) }
                ) {
                    DialWithPushers(
                        side = side,
                        rotation = rotation,
                        chronoState = chronoState,
                        chronoMillisProvider = chronoMillisProvider,
                        onChronoStartStop = vm::chronoStartStop,
                        onChronoReset = vm::chronoReset
                    )
                }
            }
        } else {
            // Compact / portrait: SubcomposeLayout measures the input
            // panels FIRST, derives a container height that guarantees
            // the panels' inner corners sit outside the dial's safety
            // circle by construction, then composes the dial at that
            // size. Single pass — no reactive feedback, no first-frame
            // flash where overlap would otherwise be zero. At fontScale
            // 1.0 on a typical phone the input panels are small enough
            // that the computed container height equals the dial's
            // natural square size, so the visual result is identical
            // to the previous reactive-state implementation. At higher
            // font scales the container grows JUST enough to clear the
            // safety circle, and never less.
            DialWithCornerInputs(
                rotation = rotation,
                chronoState = chronoState,
                chronoMillisProvider = chronoMillisProvider,
                onBezelDrag = vm::rotateBy,
                onChronoStartStop = vm::chronoStartStop,
                onChronoReset = vm::chronoReset,
                bezelInputs = bezelInputs,
                converterInputs = converterInputs
            )
        }
    }
}

/**
 * Dial + the two chronograph-pusher tap targets, sized to the given [side].
 * Pulled out so the tablet branch (dial in its own aspect-ratio Box) and
 * the portrait SubcomposeLayout branch share the same content.
 */
@Composable
private fun DialWithPushers(
    side: androidx.compose.ui.unit.Dp,
    rotation: Double,
    chronoState: com.navitimerguide.viewmodel.ChronoState,
    chronoMillisProvider: () -> Long,
    onChronoStartStop: () -> Unit,
    onChronoReset: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
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
            onChronoStartStop()
        })
    // Bottom pusher tap target (reset)
    Box(modifier = Modifier
        .offset(x = side * 0.85f, y = side * 0.67f)
        .size(width = side * 0.13f, height = side * 0.13f)
        .clickable {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onChronoReset()
        })
}

/**
 * Single-pass layout used in compact / portrait mode. Measures the two
 * input panels first, then sizes the container so the panels' inner
 * corners sit OUTSIDE a circle of radius (rOuter + safetyGap) centred
 * on the dial — guaranteed no-overlap by construction. Place ordering:
 *   - dial: top-centre, square (parentWidth × parentWidth)
 *   - bezel inputs: bottom-start, anchored to container bottom
 *   - converter inputs: bottom-end, anchored to container bottom
 *
 * At fontScale 1.0 on a typical phone the required container height
 * equals parentWidth, so the panels sit in the bottom corners of the
 * watch square — visually identical to the previous behaviour. At
 * higher font scales the container grows downward only as much as it
 * needs to keep the panels out of the safety circle.
 */
@Composable
private fun DialWithCornerInputs(
    rotation: Double,
    chronoState: com.navitimerguide.viewmodel.ChronoState,
    chronoMillisProvider: () -> Long,
    onBezelDrag: (Double) -> Unit,
    onChronoStartStop: () -> Unit,
    onChronoReset: () -> Unit,
    bezelInputs: @Composable () -> Unit,
    converterInputs: @Composable () -> Unit
) {
    SubcomposeLayout(modifier = Modifier.fillMaxWidth()) { constraints ->
        val parentWidth = constraints.maxWidth
        check(parentWidth != Constraints.Infinity) {
            "DialWithCornerInputs requires bounded width"
        }

        // 1. Measure the two input panels under loose half-width
        //    constraints (no panel may consume more than half the row).
        val looseInputConstraints = Constraints(maxWidth = parentWidth / 2)
        val bezelP = subcompose("bezel") { bezelInputs() }
            .first().measure(looseInputConstraints)
        val converterP = subcompose("converter") { converterInputs() }
            .first().measure(looseInputConstraints)

        // 2. Dial occupies a square of side = parentWidth.
        val dialSide = parentWidth
        val rOuter = (dialSide * 0.5f * 0.88f).toInt()
        val safetyPx = 12.dp.toPx().toInt()
        val safety = rOuter + safetyPx
        val centre = dialSide / 2
        val safetySq = safety.toLong() * safety.toLong()

        // 3. Required container height so the panel's inner corner clears
        //    the safety circle. Box anchored to container bottom: top
        //    edge at y = containerHeight - h; closest x toward centre at
        //    x = clamp(centre, 0, w). We need
        //      (centre - x_clamped)² + (containerH - h - centre)² ≥ safety²
        //    Solve for containerH (taking containerH - h ≥ centre branch):
        //      containerH ≥ h + centre + √(max(0, safety² - dx²))
        //    If dx ≥ safety the box's near edge is already outside the
        //    circle horizontally — containerH need only be dialSide.
        fun requiredHeight(w: Int, h: Int): Int {
            val dx = maxOf(0, centre - w)
            val dxSq = dx.toLong() * dx.toLong()
            if (dxSq >= safetySq) return dialSide
            val dyMin = kotlin.math.sqrt((safetySq - dxSq).toDouble()).toInt()
            return maxOf(dialSide, h + centre + dyMin)
        }
        val containerHeight = maxOf(
            requiredHeight(bezelP.width, bezelP.height),
            requiredHeight(converterP.width, converterP.height)
        )

        // 4. Now compose & measure the dial at exactly dialSide × dialSide.
        val sideDp = dialSide.toDp()
        val dialP = subcompose("dial") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .bezelDragRotation { onBezelDrag(it) }
            ) {
                DialWithPushers(
                    side = sideDp,
                    rotation = rotation,
                    chronoState = chronoState,
                    chronoMillisProvider = chronoMillisProvider,
                    onChronoStartStop = onChronoStartStop,
                    onChronoReset = onChronoReset
                )
            }
        }.first().measure(Constraints.fixed(dialSide, dialSide))

        layout(parentWidth, containerHeight) {
            dialP.placeRelative(0, 0)
            bezelP.placeRelative(0, containerHeight - bezelP.height)
            converterP.placeRelative(
                parentWidth - converterP.width,
                containerHeight - converterP.height
            )
        }
    }
}
