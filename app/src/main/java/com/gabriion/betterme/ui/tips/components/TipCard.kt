package com.gabriion.betterme.ui.tips.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gabriion.betterme.domain.tips.ComposedTip

@Composable
fun TipCard(tip: ComposedTip, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = iconFor(tip.kind),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = titleFor(tip.kind),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = tip.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun iconFor(kind: String): ImageVector = when (kind) {
    "sleep" -> Icons.Outlined.Bed
    "rhr" -> Icons.Outlined.Favorite
    "activity_hr" -> Icons.Outlined.DirectionsRun
    "stress" -> Icons.Outlined.Spa
    "hrv" -> Icons.Outlined.MonitorHeart
    "steps" -> Icons.Outlined.DirectionsWalk
    "hydration" -> Icons.Outlined.LocalDrink
    "mindfulness" -> Icons.Outlined.SelfImprovement
    else -> Icons.Outlined.Lightbulb
}

fun titleFor(kind: String): String = when (kind) {
    "sleep" -> "Sleep & rest"
    "rhr" -> "Heart rate"
    "activity_hr" -> "Training load"
    "stress" -> "Stress"
    "hrv" -> "Recovery"
    "steps" -> "Movement"
    "hydration" -> "Hydration"
    "mindfulness" -> "Mindfulness"
    else -> "Tip"
}
