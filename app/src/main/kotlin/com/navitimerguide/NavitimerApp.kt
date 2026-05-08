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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.navitimerguide.controls.AngleSlider
import com.navitimerguide.controls.Presets
import com.navitimerguide.dial.WatchDial
import com.navitimerguide.dial.bezelDragRotation
import com.navitimerguide.equations.BezelInputs
import com.navitimerguide.equations.FloatingEquations
import com.navitimerguide.viewmodel.DialViewModel

@Composable
fun NavitimerApp() {
    val vm: DialViewModel = viewModel()
    val rotation by vm.rotationDegrees.collectAsStateWithLifecycle()
    val outerText by vm.outerInput.collectAsStateWithLifecycle()
    val innerText by vm.innerInput.collectAsStateWithLifecycle()
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
                        vm = vm
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BezelInputs(
                            outer = outerText,
                            inner = innerText,
                            onOuterChange = vm::setOuterText,
                            onInnerChange = vm::setInnerText,
                            onCommit = vm::commitInputs
                        )
                        FloatingEquations(
                            rotationDegrees = rotation,
                            outer = outerText,
                            inner = innerText
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DialColumn(
                        modifier = Modifier.fillMaxWidth(),
                        rotation = rotation,
                        chronoState = chronoState,
                        chronoMillisProvider = vm::currentChronoMs,
                        vm = vm
                    )
                    BezelInputs(
                        outer = outerText,
                        inner = innerText,
                        onOuterChange = vm::setOuterText,
                        onInnerChange = vm::setInnerText,
                        onCommit = vm::commitInputs
                    )
                    FloatingEquations(
                        rotationDegrees = rotation,
                        outer = outerText,
                        inner = innerText
                    )
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
    vm: DialViewModel
) {
    val haptics = LocalHapticFeedback.current

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .bezelDragRotation { vm.rotateBy(it) }
        ) {
            val side = maxWidth
            WatchDial(
                bezelRotationDegrees = rotation,
                chronoState = chronoState,
                chronoMillisProvider = chronoMillisProvider,
                modifier = Modifier.fillMaxSize()
            )
            // Top pusher tap target — start / stop
            Box(modifier = Modifier
                .offset(x = side * 0.90f, y = side * 0.27f)
                .size(width = side * 0.12f, height = side * 0.10f)
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.chronoStartStop()
                })
            // Bottom pusher tap target — reset
            Box(modifier = Modifier
                .offset(x = side * 0.90f, y = side * 0.61f)
                .size(width = side * 0.12f, height = side * 0.10f)
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.chronoReset()
                })
        }
        Spacer(Modifier.size(10.dp))
        Presets(onSetAngle = vm::setRotation, onReset = vm::reset, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.size(8.dp))
        AngleSlider(angle = rotation, onAngleChange = vm::setRotation, modifier = Modifier.fillMaxWidth())
    }
}
