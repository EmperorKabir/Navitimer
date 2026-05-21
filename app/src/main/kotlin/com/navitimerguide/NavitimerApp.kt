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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.navitimerguide.controls.StayAnywhereBottomSheet
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
    // Capture method references ONCE so Compose treats subsequent
    // recompositions as stable. Without this, `vm::currentChronoMs`
    // (and similar) allocate a new KFunction each composition,
    // marking WatchDial / LiveHandsLayer params as unstable and
    // forcing the whole dial subtree to recompose on every parent
    // change.
    val chronoMillis = remember(vm) { vm::currentChronoMs }

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
                WideLayout(
                    rotation = rotation,
                    chronoState = chronoState,
                    chronoMillisProvider = chronoMillis,
                    outerText = outerText,
                    innerText = innerText,
                    statText = statText,
                    nautText = nautText,
                    kmText = kmText,
                    equationsScroll = equationsScroll,
                    vm = vm
                )
            } else {
                // Compact / portrait: dial + buttons stay anchored at the
                // top; the live equations panel is a stay-anywhere
                // bottom sheet the user can drag to any height. Peek bar
                // shows a small "Live equations" title above the bottom
                // edge. Dragging up reveals more; releasing leaves the
                // sheet where the user let go (only snaps when very near
                // the top or bottom of its travel).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        DialColumn(
                            modifier = Modifier.fillMaxWidth(),
                            rotation = rotation,
                            chronoState = chronoState,
                            chronoMillisProvider = chronoMillis,
                            outerText = outerText,
                            innerText = innerText,
                            statText = statText,
                            nautText = nautText,
                            kmText = kmText,
                            vm = vm
                        )
                    }
                    StayAnywhereBottomSheet(
                        title = "Live equations",
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 4.dp, vertical = 4.dp),
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
}

/**
 * Portrait / compact-width dial column: presets + dial-with-corner-inputs
 * stacked vertically. The dial + inputs use a SubcomposeLayout that
 * guarantees the inputs never overlap the dial by construction.
 */
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
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CurvedPresets(
            onSetAngle = vm::setRotation,
            onReset = vm::reset,
            onNudge = vm::nudgeToNearestInteger,
            modifier = Modifier.fillMaxWidth()
        )

        DialWithCornerInputs(
            rotation = rotation,
            chronoState = chronoState,
            chronoMillisProvider = chronoMillisProvider,
            onBezelDrag = vm::rotateBy,
            onChronoStartStop = vm::chronoStartStop,
            onChronoReset = vm::chronoReset,
            bezelInputs = {
                BezelInputs(
                    outer = outerText,
                    inner = innerText,
                    onOuterChange = vm::setOuterText,
                    onInnerChange = vm::setInnerText,
                    onCommit = vm::commitInputs
                )
            },
            converterInputs = {
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
        )
    }
}

/**
 * Tablet / wide-canvas layout (single-pass SubcomposeLayout). Measures
 * the presets, dial, input panels and equations panel up-front, then
 * decides at layout time whether the inputs Row sits BELOW the dial in
 * the left column (preferred when there's enough vertical room — e.g.
 * Fold 7 unfolded at fontScale 1.0) or moves to the TOP of the right
 * (equations) column when the left column would otherwise overflow.
 *
 * Robust under:
 *   - foldable open / close (recomposes on parent maxWidth/maxHeight change)
 *   - system fontScale change (re-measures inputs at new sizes; the
 *     placement decision re-evaluates accordingly)
 *   - configuration changes (subcompose slot IDs are stable strings;
 *     ViewModel + rememberScrollState survive across recomposition)
 */
@Composable
private fun WideLayout(
    rotation: Double,
    chronoState: com.navitimerguide.viewmodel.ChronoState,
    chronoMillisProvider: () -> Long,
    outerText: String,
    innerText: String,
    statText: String,
    nautText: String,
    kmText: String,
    equationsScroll: androidx.compose.foundation.ScrollState,
    vm: DialViewModel
) {
    SubcomposeLayout(modifier = Modifier.fillMaxSize().padding(12.dp)) { constraints ->
        val totalW = constraints.maxWidth
        val totalH = constraints.maxHeight
        val spacerPx = 12.dp.roundToPx()
        val colW = ((totalW - spacerPx) / 2).coerceAtLeast(0)
        val dialBelowGapPx = 8.dp.roundToPx()
        val inputsBelowMarginPx = 8.dp.roundToPx()
        val rightColInputsGapPx = 10.dp.roundToPx()

        // Subcompose & measure the presets header at its natural content
        // height (grows to fit wrapped Nudge label).
        val presetsP = subcompose("presets") {
            CurvedPresets(
                onSetAngle = vm::setRotation,
                onReset = vm::reset,
                onNudge = vm::nudgeToNearestInteger,
                modifier = Modifier.fillMaxWidth()
            )
        }.first().measure(Constraints(maxWidth = colW))

        // Dial square: side = column width.
        val dialSize = colW
        val dialP = subcompose("dial") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .bezelDragRotation { vm.rotateBy(it) }
            ) {
                DialWithPushers(
                    side = dialSize.toDp(),
                    rotation = rotation,
                    chronoState = chronoState,
                    chronoMillisProvider = chronoMillisProvider,
                    onChronoStartStop = vm::chronoStartStop,
                    onChronoReset = vm::chronoReset
                )
            }
        }.first().measure(Constraints.fixed(dialSize, dialSize))

        // Inputs Row, measured at column width (it'll be placed in whichever
        // column it ends up in — both columns are the same width).
        val inputsP = subcompose("inputs") {
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
        }.first().measure(Constraints(maxWidth = colW))

        // Fit test: does the inputs Row fit BELOW the dial in the left
        // column, with a small bottom margin? If yes, place it there.
        // Otherwise it moves to the top of the right column.
        val leftStackH = presetsP.height + dialP.height + dialBelowGapPx +
            inputsP.height + inputsBelowMarginPx
        val inputsBelowDial = leftStackH <= totalH

        // Equations panel fills the rest of the right column. When the
        // inputs are on the right too, the equations panel gets less
        // height; the verticalScroll handles overflow gracefully.
        val rightTopUsed = if (inputsBelowDial) 0 else inputsP.height + rightColInputsGapPx
        val eqMaxH = (totalH - rightTopUsed).coerceAtLeast(0)
        val equationsP = subcompose("equations") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
        }.first().measure(Constraints(maxWidth = colW, maxHeight = eqMaxH))

        layout(totalW, totalH) {
            // ----- Left column -----
            presetsP.placeRelative(0, 0)
            dialP.placeRelative(0, presetsP.height)
            if (inputsBelowDial) {
                inputsP.placeRelative(0, presetsP.height + dialP.height + dialBelowGapPx)
            }

            // ----- Right column -----
            val rightX = colW + spacerPx
            if (inputsBelowDial) {
                equationsP.placeRelative(rightX, 0)
            } else {
                inputsP.placeRelative(rightX, 0)
                equationsP.placeRelative(rightX, inputsP.height + rightColInputsGapPx)
            }
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
