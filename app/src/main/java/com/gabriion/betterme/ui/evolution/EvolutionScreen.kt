package com.gabriion.betterme.ui.evolution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gabriion.betterme.ui.evolution.components.AddWeightSheet
import com.gabriion.betterme.ui.evolution.components.HighlightCard
import com.gabriion.betterme.ui.evolution.components.RecentWeightRow
import com.gabriion.betterme.ui.evolution.components.WeightChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvolutionScreen(viewModel: EvolutionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showRecent by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log weight")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item { Header(state, onLog = { showAdd = true }) }
                item {
                    WindowSelector(
                        selected = state.window,
                        onSelect = viewModel::setWindow,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    WeightChart(
                        weights = state.weights,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                if (state.highlights.isNotEmpty()) {
                    item {
                        Text(
                            "Highlights",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(state.highlights, key = { it.title }) { h ->
                        HighlightCard(highlight = h)
                    }
                }

                item {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    RecentEntriesHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        showRecent = showRecent,
                        onToggle = { showRecent = !showRecent },
                        count = state.weights.size
                    )
                }

                if (showRecent) {
                    items(state.weights.sortedByDescending { it.date }, key = { it.id }) { entry ->
                        RecentWeightRow(
                            entry = entry,
                            onDelete = viewModel::deleteWeight
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAdd) {
        AddWeightSheet(
            onDismiss = { showAdd = false },
            onSave = { date, kg, note ->
                viewModel.addWeight(date, kg, note)
                showAdd = false
            }
        )
    }
}

@Composable
private fun Header(state: EvolutionUiState, onLog: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            "Your progress",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        val latest = state.latestKg
        if (latest != null) {
            Text(
                "${"%.1f".format(latest)} kg",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onLog) { Text("Log your first weight") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WindowSelector(
    selected: TimeWindow,
    onSelect: (TimeWindow) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember { TimeWindow.values().toList() }
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, window ->
            SegmentedButton(
                selected = window == selected,
                onClick = { onSelect(window) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(window.label)
            }
        }
    }
}

@Composable
private fun RecentEntriesHeader(
    modifier: Modifier = Modifier,
    showRecent: Boolean,
    onToggle: () -> Unit,
    count: Int
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Recent entries ($count)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        androidx.compose.material3.TextButton(onClick = onToggle) {
            Text(if (showRecent) "Hide" else "Show")
        }
    }
}
