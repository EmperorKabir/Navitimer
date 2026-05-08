package com.navitimerguide.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
 * One-tap presets: each chip rotates the bezel to a useful alignment with
 * a single tap. Wraps onto multiple lines (FlowRow) so long lists don't
 * scroll horizontally.
 *
 * Labels are written for non-experts: each chip says what the alignment
 * *does*, not the technical "outer→inner" form.
 */
@OptIn(ExperimentalLayoutApi::class)
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
        Text("Quick examples", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.size(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chip("Reset") {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onReset()
            }
            chip("Multiply by 2.5") {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSetAngle(DialMath.alignRotation(25.0, 10.0))
            }
            chip("Multiply by 3.5") {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSetAngle(DialMath.alignRotation(35.0, 10.0))
            }
            chip("Hours → minutes & seconds") {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSetAngle(DialMath.alignRotation(40.0, 10.0))  // 4-hour worked example
            }
            chip("Miles → kilometres") {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSetAngle(DialMath.alignRotation(10.0, DialMath.STAT_MARKER))
            }
            chip("Nautical miles → km") {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSetAngle(DialMath.alignRotation(10.0, DialMath.NAUT_MARKER))
            }
        }
    }
}

@Composable
private fun chip(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) })
}
