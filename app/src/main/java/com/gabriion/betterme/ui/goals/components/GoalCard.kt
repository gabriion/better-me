package com.gabriion.betterme.ui.goals.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriion.betterme.domain.goals.PaceState
import com.gabriion.betterme.ui.goals.GoalWithProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalCard(
    item: GoalWithProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressRing(percent = item.summary.percent)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.goal.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = remainingText(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaceChip(item.summary.paceState)
                    if (item.goal.isSportTracked) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Garmin", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaceChip(pace: PaceState) {
    val (label, container, content) = when (pace) {
        PaceState.AHEAD -> Triple("Ahead", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        PaceState.ON_TRACK -> Triple("On track", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        PaceState.BEHIND -> Triple("Behind", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        PaceState.COMPLETE -> Triple("Complete", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
    }
    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(containerColor = container, labelColor = content)
    )
}

private fun remainingText(item: GoalWithProgress): String {
    val total = formatNumber(item.total)
    val target = formatNumber(item.goal.target)
    val unit = item.goal.unit
    return "$total of $target $unit"
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)
