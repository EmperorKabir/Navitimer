package com.navitimerguide.equations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tiny stacked Outer / Inner inputs designed to fit in the bottom-left
 * corner GAP between the watch circle and the box edge, without overlapping
 * the dial.
 */
@Composable
fun BezelInputs(
    outer: String,
    inner: String,
    onOuterChange: (String) -> Unit,
    onInnerChange: (String) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        TinyLabelledField(
            label = "Outer",
            value = outer,
            onValueChange = onOuterChange,
            onCommit = onCommit,
            imeAction = ImeAction.Next
        )
        TinyLabelledField(
            label = "Inner",
            value = inner,
            onValueChange = onInnerChange,
            onCommit = onCommit,
            imeAction = ImeAction.Done
        )
    }
}

@Composable
private fun TinyLabelledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    imeAction: ImeAction
) {
    var hadFocus by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(34.dp)
        )
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 6.dp, vertical = 3.dp)
                .onFocusChanged { focus ->
                    if (focus.isFocused) hadFocus = true
                    else if (hadFocus) {
                        hadFocus = false
                        onCommit()
                    }
                }
        ) {
            BasicTextField(
                value = value,
                onValueChange = { raw ->
                    onValueChange(raw.filter { it.isDigit() || it == '.' })
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(onDone = { onCommit() }),
                modifier = Modifier.width(40.dp).heightIn(min = 16.dp)
            )
        }
    }
}
