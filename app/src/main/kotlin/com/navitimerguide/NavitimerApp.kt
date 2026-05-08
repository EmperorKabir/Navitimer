package com.navitimerguide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.navitimerguide.controls.AlignInput
import com.navitimerguide.controls.AngleSlider
import com.navitimerguide.controls.StepArrows
import com.navitimerguide.dial.WatchDial
import com.navitimerguide.dial.bezelDragRotation
import com.navitimerguide.equations.EquationsPanel
import com.navitimerguide.viewmodel.DialViewModel

@Composable
fun NavitimerApp() {
    val vm: DialViewModel = viewModel()
    val rotation by vm.rotationDegrees.collectAsStateWithLifecycle()
    val groups = remember(rotation) { vm.equationsFor(rotation) }

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
                        onRotate = vm::rotateBy,
                        onSetAngle = vm::setRotation,
                        onSnap = vm::snapAlign,
                        onReset = vm::reset
                    )
                    Spacer(Modifier.size(12.dp))
                    EquationsPanel(
                        groups = groups,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DialColumn(
                        modifier = Modifier.fillMaxWidth(),
                        rotation = rotation,
                        onRotate = vm::rotateBy,
                        onSetAngle = vm::setRotation,
                        onSnap = vm::snapAlign,
                        onReset = vm::reset
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Equations (live)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    )
                    // Render groups inline (not LazyColumn) so the outer scroll handles it.
                    groups.forEach { g -> com.navitimerguide.equations.GroupCardInline(g) }
                }
            }
        }
    }
}

@Composable
private fun DialColumn(
    modifier: Modifier,
    rotation: Double,
    onRotate: (Double) -> Unit,
    onSetAngle: (Double) -> Unit,
    onSnap: (Double, Double) -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .bezelDragRotation { onRotate(it) },
            contentAlignment = Alignment.Center
        ) {
            WatchDial(bezelRotationDegrees = rotation)
        }
        Spacer(Modifier.size(10.dp))
        StepArrows(onStep = onRotate, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.size(8.dp))
        AngleSlider(angle = rotation, onAngleChange = onSetAngle, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.size(8.dp))
        AlignInput(
            onSnapAlign = onSnap,
            onReset = onReset,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
