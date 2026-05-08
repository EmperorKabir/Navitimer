package com.navitimerguide.equations.cards

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * One editable variable in an equation card.
 *
 * - [text] is the controlled input string (the parent owns it so cards can
 *   recompute dependent fields as the user types).
 * - [onChange] fires on every keystroke; the card uses it to refresh
 *   dependent fields without yet rotating the bezel.
 * - [onCommit] fires on focus loss or IME Done — the card uses it to
 *   rotate the bezel to demonstrate the alignment.
 */
@Composable
fun EquationField(
    text: String,
    onChange: (String) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    suffix: String? = null,
    enabled: Boolean = true,
) {
    var hadFocus by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            onChange(raw.filter { c -> c.isDigit() || c == '.' || c == '-' })
        },
        modifier = modifier
            .padding(horizontal = 2.dp)
            .onFocusChanged { focus ->
                if (focus.isFocused) hadFocus = true
                else if (hadFocus) {
                    hadFocus = false
                    onCommit()
                }
            },
        enabled = enabled,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
        label = label?.let { { Text(it, style = MaterialTheme.typography.labelSmall) } },
        suffix = suffix?.let { { Text(it, style = MaterialTheme.typography.labelSmall) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onCommit() })
    )
}
