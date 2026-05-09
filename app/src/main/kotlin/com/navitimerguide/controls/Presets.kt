package com.navitimerguide.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.navitimerguide.dial.DialMath

/**
 * Preset chips arranged in a gentle curve along the upper edge of the
 * watch — the chip in the middle sits highest (at 12 o'clock), chips
 * on the sides drop down to follow the top arc. Heading removed.
 */
@Composable
fun CurvedPresets(
    onSetAngle: (Double) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val items = listOf(
        "Reset" to onReset,
        "× 2.5" to { onSetAngle(DialMath.alignRotation(25.0, 10.0)) },
        "× 3.5" to { onSetAngle(DialMath.alignRotation(35.0, 10.0)) },
        "Hours" to { onSetAngle(DialMath.alignRotation(40.0, 10.0)) },
        "mi → km" to { onSetAngle(DialMath.alignRotation(10.0, DialMath.STAT_MARKER)) },
        "nm → km" to { onSetAngle(DialMath.alignRotation(10.0, DialMath.NAUT_MARKER)) }
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(64.dp)) {
        val w = maxWidth
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            items.forEachIndexed { i, (label, onClick) ->
                val n = items.size
                val t = i.toDouble() / (n - 1).coerceAtLeast(1) - 0.5
                // Parabolic drop: middle chips at y=0, sides drop down ~16dp.
                val drop = (t * t * 64.0).dp
                AssistChip(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
                    label = {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                        )
                    },
                    modifier = Modifier.offset(y = drop),
                    colors = AssistChipDefaults.assistChipColors()
                )
            }
        }
    }
}

// Backward-compatible name kept in case other code imports it.
@Composable
fun Presets(
    onSetAngle: (Double) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) = CurvedPresets(onSetAngle, onReset, modifier)
