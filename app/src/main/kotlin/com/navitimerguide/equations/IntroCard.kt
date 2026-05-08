package com.navitimerguide.equations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun IntroCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "How to use",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.size(6.dp))
            Text(
                "Drag the outer bezel to rotate it (or use the slider, ± arrows, " +
                "or type an alignment). Every group below recomputes live from " +
                "the current bezel angle. The slide rule is logarithmic, so the " +
                "spacing isn't even — that's how ratios become rotations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "Decimal-point placement is up to you: a reading of 25 might mean " +
                "2.5, 25 or 250 depending on context — just like the real watch.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
