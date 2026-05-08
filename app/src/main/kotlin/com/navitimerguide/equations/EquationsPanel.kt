package com.navitimerguide.equations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EquationsPanel(
    groups: List<EquationGroup>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(groups, key = { it.title }) { group -> GroupCard(group) }
    }
}

@Composable
private fun GroupCard(group: EquationGroup) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
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
                EquationRowView(row)
            }
        }
    }
}

@Composable
private fun EquationRowView(row: EquationRow) {
    val highlightColor = if (row.highlight) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface
    val fontWeight = if (row.highlight) FontWeight.SemiBold else FontWeight.Normal
    Text(
        text = row.text,
        style = MaterialTheme.typography.bodyMedium,
        color = highlightColor,
        fontWeight = fontWeight,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
