package com.gabriion.betterme.ui.calories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.gabriion.betterme.core.db.FoodEntryEntity
import com.gabriion.betterme.ui.calories.components.AddEntrySheet
import com.gabriion.betterme.ui.calories.components.EntryRow
import com.gabriion.betterme.ui.calories.components.KcalRing
import com.gabriion.betterme.ui.theme.Cream
import com.gabriion.betterme.ui.theme.Ink
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaloriesScreen(viewModel: CaloriesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add food")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                DayHeader(
                    date = state.date,
                    onPrev = { viewModel.goPreviousDay() },
                    onNext = { viewModel.goNextDay() },
                    onToday = { viewModel.goToday() }
                )
            }
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    KcalRing(totalKcal = state.totalKcal, targetKcal = state.dailyTarget)
                }
            }
            item { MacrosRow(entries = state.entries) }
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Today's meals",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            if (state.entries.isEmpty()) {
                item { EmptyMealsCard(onAdd = { showAdd = true }) }
            } else {
                val grouped = state.entries.groupBy { it.slot }
                val slotOrder = listOf("breakfast", "lunch", "dinner", "snack")
                slotOrder.forEach { slot ->
                    val rows = grouped[slot].orEmpty()
                    if (rows.isNotEmpty()) {
                        item {
                            Text(
                                slot.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(rows, key = { it.id }) { entry ->
                            EntryRow(entry = entry, onDelete = { viewModel.deleteEntry(entry.id) })
                        }
                    }
                }
                // Any "other" slot values are still rendered, after the canonical four.
                grouped.keys
                    .filter { it !in slotOrder }
                    .forEach { slot ->
                        val rows = grouped[slot].orEmpty()
                        item {
                            Text(
                                slot.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(rows, key = { it.id }) { entry ->
                            EntryRow(entry = entry, onDelete = { viewModel.deleteEntry(entry.id) })
                        }
                    }
            }
        }
    }

    if (showAdd) {
        AddEntrySheet(
            state = state,
            onDismiss = { showAdd = false },
            onQueryChange = viewModel::onQueryChange,
            onToggleFavourite = viewModel::toggleFavourite,
            onLog = { cache, grams, slot -> viewModel.logEntry(cache, grams, slot) },
            onLogManual = { name, kcal, grams, slot -> viewModel.logManual(name, kcal, grams, slot) }
        )
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val isToday = date == LocalDate.now()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(date.format(formatter), style = MaterialTheme.typography.titleMedium)
            if (!isToday) {
                Text(
                    "Tap to jump to today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 2.dp)
                )
                Button(
                    onClick = onToday,
                    modifier = Modifier.padding(top = 4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) { Text("Today") }
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
        }
    }
}

@Composable
private fun MacrosRow(entries: List<FoodEntryEntity>) {
    val protein = entries.sumOf { it.proteinG }
    val carbs = entries.sumOf { it.carbsG }
    val fat = entries.sumOf { it.fatG }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(onClick = {}, label = { Text("Protein ${"%.0f".format(protein)}g") })
        AssistChip(onClick = {}, label = { Text("Carbs ${"%.0f".format(carbs)}g") })
        AssistChip(onClick = {}, label = { Text("Fat ${"%.0f".format(fat)}g") })
    }
}

@Composable
private fun EmptyMealsCard(onAdd: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Cream, contentColor = Ink)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🥗", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text("Log your first meal", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Search foods or add a quick manual entry.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAdd) { Text("Add food") }
        }
    }
}
