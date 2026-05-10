package com.navitimerguide.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.navitimerguide.dial.DialMath

/**
 * Header row above the watch. Layout:
 *
 *   [ Reset ]   ⌒ Examples ⌒
 *               × 2.5  × 3.5  Hours  mi → km  nm → km
 *
 * Reset sits on its own at the LEFT in a slightly heavier chip style. The
 * five example chips are grouped under an "EXAMPLES" caption on the RIGHT
 * and arranged in a gentle parabolic arc so they trace the top of the
 * watch face below them.
 */
@Composable
fun CurvedPresets(
    onSetAngle: (Double) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val examples = listOf(
        "× 2.5"   to { onSetAngle(DialMath.alignRotation(25.0, 10.0)) },
        "× 3.5"   to { onSetAngle(DialMath.alignRotation(35.0, 10.0)) },
        "Hours"   to { onSetAngle(DialMath.alignRotation(40.0, 10.0)) },
        "mi → km" to { onSetAngle(DialMath.alignRotation(10.0, DialMath.STAT_MARKER)) },
        "nm → km" to { onSetAngle(DialMath.alignRotation(10.0, DialMath.NAUT_MARKER)) }
    )

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // ----- LEFT: Reset on its own, slightly emphasised -----
        Box(modifier = Modifier.padding(top = 6.dp)) {
            AssistChip(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onReset()
                },
                label = {
                    Text(
                        "Reset",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        Spacer(Modifier.width(8.dp))

        // ----- RIGHT: Examples header + arched chip row -----
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "EXAMPLES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().height(58.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    examples.forEachIndexed { i, (label, onClick) ->
                        val n = examples.size
                        val t = i.toDouble() / (n - 1).coerceAtLeast(1) - 0.5
                        // Parabolic drop — middle chips highest, sides drop down.
                        val drop = (t * t * 38.0).dp
                        AssistChip(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClick()
                            },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp
                                    )
                                )
                            },
                            modifier = Modifier.offset(y = drop),
                            colors = AssistChipDefaults.assistChipColors()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Presets(
    onSetAngle: (Double) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) = CurvedPresets(onSetAngle, onReset, modifier)
