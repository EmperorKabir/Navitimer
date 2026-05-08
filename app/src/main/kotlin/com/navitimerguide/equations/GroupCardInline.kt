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

/**
 * Variant of GroupCard for use inside an outer Column.verticalScroll
 * (so we don't nest scrolls).
 */
@Composable
fun GroupCardInline(group: EquationGroup) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = group.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(8.dp))
            group.rows.forEach { row ->
                Text(
                    text = row.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (row.highlight) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (row.highlight) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
