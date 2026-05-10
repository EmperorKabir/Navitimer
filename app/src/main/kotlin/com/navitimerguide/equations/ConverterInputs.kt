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
 * STAT mi / NAUT mi / KM converter trio on the bottom-RIGHT corner gap of
 * the watch. Each field commits independently — the bezel snaps so the
 * typed value sits above the corresponding marker (STAT, NAUT, KM), and
 * the other two fields refresh from the new rotation.
 */
@Composable
fun ConverterInputs(
    stat: String,
    naut: String,
    km: String,
    onStatChange: (String) -> Unit,
    onNautChange: (String) -> Unit,
    onKmChange: (String) -> Unit,
    onCommitStat: () -> Unit,
    onCommitNaut: () -> Unit,
    onCommitKm: () -> Unit,
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
            label = "Stat mi",
            value = stat,
            onValueChange = onStatChange,
            onCommit = onCommitStat,
            imeAction = ImeAction.Next,
            labelWidthDp = 44
        )
        LabelledFieldRow(
            label = "Naut mi",
            value = naut,
            onValueChange = onNautChange,
            onCommit = onCommitNaut,
            imeAction = ImeAction.Next,
            labelWidthDp = 44
        )
        LabelledFieldRow(
            label = "KM",
            value = km,
            onValueChange = onKmChange,
            onCommit = onCommitKm,
            imeAction = ImeAction.Done,
            labelWidthDp = 44
        )
    }
}
