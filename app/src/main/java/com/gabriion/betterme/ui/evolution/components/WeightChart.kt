package com.gabriion.betterme.ui.evolution.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriion.betterme.core.db.WeightEntity
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

// TODO refine for Vico 1.13 — minimal defaults used; M3 theming inherited from compose-m3.
@Composable
fun WeightChart(
    weights: List<WeightEntity>,
    modifier: Modifier = Modifier
) {
    if (weights.isEmpty()) {
        EmptyChartCard(modifier)
        return
    }

    val producer = remember { CartesianChartModelProducer.build() }
    LaunchedEffect(weights) {
        runCatching {
            producer.runTransaction {
                lineSeries { series(weights.map { it.kg }) }
            }
        }
    }

    runCatching {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis()
            ),
            modelProducer = producer,
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 12.dp)
        )
    }.onFailure {
        EmptyChartCard(modifier, label = "Chart unavailable")
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
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
