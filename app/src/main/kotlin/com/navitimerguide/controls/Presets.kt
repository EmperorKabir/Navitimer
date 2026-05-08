package com.navitimerguide.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.navitimerguide.dial.DialMath

/**
 * One-tap alignments for the most useful slide-rule moves. Each preset
 * snaps the bezel and gives a small haptic confirmation.
 */
@Composable
fun Presets(
    onSetAngle: (Double) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text("Presets — common alignments", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.size(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chip("Reset (10↔10)") {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onReset()
            }
            chip("× 2.5  (25→10)") {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSetAngle(DialMath.alignRotation(25.0, 10.0))
            }
            chip("× 3.5  (35→10)") {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSetAngle(DialMath.alignRotation(35.0, 10.0))
            }
            chip("4 hours  (40→10)") {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSetAngle(DialMath.alignRotation(40.0, 10.0))
            }
            chip("mi → km  (10→STAT)") {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSetAngle(DialMath.alignRotation(10.0, DialMath.STAT_MARKER))
            }
            chip("nm → km  (10→NAUT)") {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSetAngle(DialMath.alignRotation(10.0, DialMath.NAUT_MARKER))
            }
            chip("60 mph  (60↔60)") {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSetAngle(DialMath.alignRotation(60.0, 60.0))
            }
        }
    }
}

@Composable
private fun chip(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) })
}
