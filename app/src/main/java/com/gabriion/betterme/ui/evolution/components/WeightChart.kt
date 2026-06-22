package com.gabriion.betterme.ui.evolution.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gabriion.betterme.core.db.WeightEntity
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * Lightweight Canvas-drawn line chart for weight over time. Avoids pulling in
 * Vico's evolving API for V1 — this is purpose-built, ~100 LoC, and matches
 * the brand palette via MaterialTheme.
 */
@Composable
fun WeightChart(
    weights: List<WeightEntity>,
    modifier: Modifier = Modifier
) {
    if (weights.isEmpty()) {
        EmptyChartCard(modifier)
        return
    }

    val sorted = weights.sortedBy { it.date }
    val minKg = sorted.minOf { it.kg }
    val maxKg = sorted.maxOf { it.kg }
    val span = (maxKg - minKg).coerceAtLeast(0.5)
    val firstDate = sorted.first().date
    val lastDate = sorted.last().date
    val daySpan = max(ChronoUnit.DAYS.between(firstDate, lastDate), 1L)

    val lineColor = MaterialTheme.colorScheme.primary
    val areaColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val dotColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // 3 horizontal grid lines
                for (i in 0..3) {
                    val y = h * i / 3f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                if (sorted.size == 1) {
                    val cx = w / 2f
                    val cy = h / 2f
                    drawCircle(color = dotColor, radius = 6f, center = Offset(cx, cy))
                    return@Canvas
                }

                val points = sorted.map { entry ->
                    val tx = ChronoUnit.DAYS.between(firstDate, entry.date).toFloat() / daySpan
                    val ty = ((entry.kg - minKg) / span).toFloat()
                    Offset(tx * w, h - ty * h)
                }

                // filled area under line
                val areaPath = Path().apply {
                    moveTo(points.first().x, h)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, h)
                    close()
                }
                drawPath(path = areaPath, color = areaColor)

                // line
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                // dots
                points.forEach { p ->
                    drawCircle(color = dotColor, radius = 5f, center = p)
                }
            }
        }
    }
}

@Composable
private fun EmptyChartCard(modifier: Modifier = Modifier, label: String = "Add weights to see your trend") {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
