package com.navitimerguide.equations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Outer / Inner pair on the bottom-LEFT corner gap of the watch.
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
        LabelledFieldRow(
            label = "Outer",
            value = outer,
            onValueChange = onOuterChange,
            onCommit = onCommit,
            imeAction = ImeAction.Next
        )
        LabelledFieldRow(
            label = "Inner",
            value = inner,
            onValueChange = onInnerChange,
            onCommit = onCommit,
            imeAction = ImeAction.Done
        )
    }
}
