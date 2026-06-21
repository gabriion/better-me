package com.gabriion.betterme.ui.goals.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ProgressRing(
    percent: Float,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    strokeWidth: Dp = 8.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    label: String? = null
) {
    val safePercent = percent.coerceIn(0f, 1f)
    val labelText = label ?: "${(safePercent * 100).toInt()}%"

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val diameter = this.size.minDimension - stroke
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * safePercent,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
        }
        Text(
            text = labelText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
