package com.navitimerguide.equations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Small two-field input strip for the bezel. No heading. Sized to fit
 * comfortably in the bottom-left corner of the watch overlay.
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
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        TinyField(
            value = outer,
            onValueChange = onOuterChange,
            onCommit = onCommit,
            label = "Outer",
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )
        TinyField(
            value = inner,
            onValueChange = onInnerChange,
            onCommit = onCommit,
            label = "Inner",
            imeAction = ImeAction.Done,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TinyField(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    label: String,
    imeAction: ImeAction,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() || it == '.' }) },
        label = {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
        },
        singleLine = true,
        textStyle = TextStyle(fontSize = 13.sp),
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onCommit() }, onNext = { /* keep typing */ })
    )
}
