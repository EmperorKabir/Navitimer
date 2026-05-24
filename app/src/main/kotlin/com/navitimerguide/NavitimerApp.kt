package com.navitimerguide

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
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

// Remote-sync glow colour: subtle cyan, matching the wear side. Drawn as
// an outer-edge ring on the phone dial (the wear side uses the inner
// chapter-ring edge) when a bezel rotation arrives from the partner.
private val SYNC_GLOW_COLOR = androidx.compose.ui.graphics.Color(0x804DD0E1)

/**
 * Outer-edge cyan glow overlay, alpha driven by [alpha] (0..1). Drawn at
 * the visible bezel outer radius (0.88 of the half-dimension) so it reads
 * as a halo around the dial. No-op when [alpha] is ~0.
 */
private fun Modifier.syncGlow(alpha: Float): Modifier = drawWithContent {
    drawContent()
    if (alpha <= 0.001f) return@drawWithContent
    val side = kotlin.math.min(size.width, size.height)
    val rOuter = side * 0.5f * 0.88f
    drawCircle(
        color = SYNC_GLOW_COLOR.copy(alpha = SYNC_GLOW_COLOR.alpha * alpha),
        radius = rOuter,
        center = Offset(size.width / 2f, size.height / 2f),
        style = Stroke(width = side * 0.014f),
    )
}

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

    // Remote-sync glow: alpha pulsed on each phone-applied remote
    // rotation — a subtle outer-edge cyan halo on the dial, mirroring
    // the wear-side inner-edge glow (~150 ms fade). The pulse counter is
    // declared BEFORE the sync binder so onRemoteRotation increments it
    // directly.
    val glow = remember { Animatable(0f) }
    var remotePulse by remember { mutableStateOf(0) }

    // Bidirectional bezel sync with a paired Wear OS watch. Incoming
    // remote rotations apply with an epsilon echo-guard.
    val syncState = com.navitimerguide.sync.rememberBezelSync(
        rotationFlow = vm.rotationDegrees,
        source = "phone",
        onRemoteRotation = { remote ->
            if (kotlin.math.abs(remote - vm.rotationDegrees.value) > 0.05) {
                vm.setRotation(remote)
                remotePulse++
            }
        },
    )

    LaunchedEffect(remotePulse) {
        if (remotePulse > 0) {
            glow.snapTo(1f)
            glow.animateTo(0f, tween(durationMillis = 150))
        }
    }

    var showSyncMenu by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { showSyncMenu = true })
                }
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
                    glowAlpha = glow.value,
                    vm = vm
                )
            } else {
                // Compact / portrait: 5-snap-point live-equations sheet.
                // P1 peek, P2 below input boxes, P3 below dial circle,
                // P4 below chip buttons, P5 full extent. All four
                // measured snaps derive from runtime layout values so
                // they remain correct across screen sizes, font scales
                // and foldable transitions.
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val sheetParentHeightDp = maxHeight
                    var chipsBottomDp by remember { mutableStateOf(0.dp) }
                    var dialCircleBottomDp by remember { mutableStateOf(0.dp) }
                    var inputsBottomDp by remember { mutableStateOf(0.dp) }
                    val density = LocalDensity.current

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
                            glowAlpha = glow.value,
                            vm = vm,
                            onDialBottomYChanged = { chipsPx, dialPx, inputsPx ->
                                val newChips = with(density) { chipsPx.toDp() }
                                val newDial = with(density) { dialPx.toDp() }
                                val newInputs = with(density) { inputsPx.toDp() }
                                if ((newChips - chipsBottomDp).value.let { kotlin.math.abs(it) } > 0.5f) {
                                    chipsBottomDp = newChips
                                }
                                if ((newDial - dialCircleBottomDp).value.let { kotlin.math.abs(it) } > 0.5f) {
                                    dialCircleBottomDp = newDial
                                }
                                if ((newInputs - inputsBottomDp).value.let { kotlin.math.abs(it) } > 0.5f) {
                                    inputsBottomDp = newInputs
                                }
                            },
                        )
                    }
                    val gapBelowChips = 2.dp
                    val gapBelowDial = 2.dp
                    val gapBelowInputs = 4.dp
                    val chipsSnapDp = (sheetParentHeightDp - chipsBottomDp - gapBelowChips)
                        .coerceAtLeast(56.dp)
                    val midSnapDp = (sheetParentHeightDp - dialCircleBottomDp - gapBelowDial)
                        .coerceAtLeast(56.dp)
                    val inputsSnapDp = (sheetParentHeightDp - inputsBottomDp - gapBelowInputs)
                        .coerceAtLeast(56.dp)
                    val fullSnapDp = sheetParentHeightDp.coerceAtLeast(chipsSnapDp)
                    StayAnywhereBottomSheet(
                        title = "Live equations",
                        snapHeightsDp = listOf(56.dp, inputsSnapDp, midSnapDp, chipsSnapDp, fullSnapDp),
                        topInsetDp = 0.dp,
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

    if (showSyncMenu) {
        SyncSettingsSheet(
            syncEnabled = syncState.syncEnabled,
            partnerAvailable = syncState.partnerAvailable,
            onToggle = { syncState.setSyncEnabled(it) },
            onDismiss = { showSyncMenu = false },
        )
    }
}

/** Long-press settings sheet exposing the bezel-sync On/Off toggle. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SyncSettingsSheet(
    syncEnabled: Boolean,
    partnerAvailable: Boolean,
    onToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            androidx.compose.material3.Text(
                text = "Bezel sync",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.Text(
                        text = "Sync bezel with watch",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    androidx.compose.material3.Text(
                        text = if (partnerAvailable) "Watch connected" else "No watch detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                androidx.compose.material3.Switch(
                    checked = syncEnabled,
                    onCheckedChange = onToggle,
                )
            }
            androidx.compose.material3.Text(
                text = "When on, turning the bezel here also turns it on your paired watch (and vice versa). Works only while both apps are installed and the watch is paired.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )
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
    glowAlpha: Float,
    vm: DialViewModel,
    onDialBottomYChanged: ((Float, Float, Float) -> Unit)? = null,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CurvedPresets(
            onSetAngle = vm::setRotation,
            onReset = vm::reset,
            onNudge = vm::nudgeToNearestInteger,
            modifier = Modifier.fillMaxWidth()
        )

        DialWithCornerInputs(
            dialBottomYReporter = onDialBottomYChanged,
            rotation = rotation,
            chronoState = chronoState,
            chronoMillisProvider = chronoMillisProvider,
            glowAlpha = glowAlpha,
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
    glowAlpha: Float,
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
                    onChronoReset = vm::chronoReset,
                    glowAlpha = glowAlpha
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
    onChronoReset: () -> Unit,
    glowAlpha: Float = 0f
) {
    val haptics = LocalHapticFeedback.current
    WatchDial(
        bezelRotationDegrees = rotation,
        chronoState = chronoState,
        chronoMillisProvider = chronoMillisProvider,
        modifier = Modifier.fillMaxSize().syncGlow(glowAlpha)
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
    glowAlpha: Float,
    bezelInputs: @Composable () -> Unit,
    converterInputs: @Composable () -> Unit,
    dialBottomYReporter: ((Float, Float, Float) -> Unit)? = null,
) {
    SubcomposeLayout(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (dialBottomYReporter != null) {
                    Modifier.onGloballyPositioned { coords ->
                        val containerTopY = coords.positionInParent().y
                        val chipsBottomY = containerTopY
                        val circleBottomY = containerTopY + coords.size.width * 0.945f
                        val containerBottomY = containerTopY + coords.size.height
                        dialBottomYReporter(chipsBottomY, circleBottomY, containerBottomY)
                    }
                } else Modifier
            )
    ) { constraints ->
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
                    onChronoReset = onChronoReset,
                    glowAlpha = glowAlpha
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
