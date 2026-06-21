package com.gabriion.betterme.ui.calories.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gabriion.betterme.core.db.FoodCacheEntity
import com.gabriion.betterme.ui.calories.CaloriesUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    state: CaloriesUiState,
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleFavourite: (String, Boolean) -> Unit,
    onLog: (FoodCacheEntity, Double, String) -> Unit,
    onLogManual: (String, Double, Double, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf(state.query) }
    var tab by remember { mutableIntStateOf(0) }
    var picker by remember { mutableStateOf<FoodCacheEntity?>(null) }
    var showManual by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        delay(350)
        if (query != state.query) onQueryChange(query)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Add food", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search foods") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            PrimaryTabRow(selectedTabIndex = tab) {
                listOf("Search", "Favourites", "Recent").forEachIndexed { i, label ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            val list: List<FoodCacheEntity> = when (tab) {
                0 -> state.searchResults
                1 -> state.favourites
                else -> state.recent
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 360.dp)
            ) {
                if (tab == 0 && state.isSearching && list.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (list.isEmpty()) {
                    val msg = when (tab) {
                        0 -> if (query.isBlank()) "Type to search foods" else "No matches"
                        1 -> "No favourites yet — tap the star on any food"
                        else -> "No recently used foods"
                    }
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn {
                        items(list, key = { it.foodId }) { item ->
                            FoodSearchRow(
                                item = item,
                                onClick = { picker = item },
                                onToggleFavourite = { fav -> onToggleFavourite(item.foodId, fav) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showManual = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add manually")
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    picker?.let { item ->
        SlotGramsDialog(
            title = item.name,
            kcalPer100g = item.kcalPer100g,
            onDismiss = { picker = null },
            onConfirm = { grams, slot ->
                onLog(item, grams, slot)
                picker = null
                onDismiss()
            }
        )
    }

    if (showManual) {
        ManualEntryDialog(
            onDismiss = { showManual = false },
            onConfirm = { name, kcal, grams, slot ->
                onLogManual(name, kcal, grams, slot)
                showManual = false
                onDismiss()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotGramsDialog(
    title: String,
    kcalPer100g: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    val slots = listOf("breakfast", "lunch", "dinner", "snack")
    var slotIdx by remember { mutableIntStateOf(1) }
    var grams by remember { mutableStateOf("100") }

    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${kcalPer100g.toInt()} kcal / 100g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                Text("Meal slot", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    slots.forEachIndexed { i, slot ->
                        SegmentedButton(
                            selected = slotIdx == i,
                            onClick = { slotIdx = i },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = slots.size),
                            label = { Text(slot.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                Text("Grams", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = grams,
                    onValueChange = { grams = it.filter { c -> c.isDigit() || c == '.' } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(100, 150, 200).forEach { q ->
                        AssistChip(onClick = { grams = q.toString() }, label = { Text("${q}g") })
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = (grams.toDoubleOrNull() ?: 0.0) > 0.0,
                        onClick = {
                            onConfirm(grams.toDouble(), slots[slotIdx])
                        }
                    ) { Text("Log") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, String) -> Unit
) {
    val slots = listOf("breakfast", "lunch", "dinner", "snack")
    var slotIdx by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("100") }

    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add manually", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kcal,
                        onValueChange = { kcal = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("kcal") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = grams,
                        onValueChange = { grams = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("grams") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))

                Text("Meal slot", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    slots.forEachIndexed { i, slot ->
                        SegmentedButton(
                            selected = slotIdx == i,
                            onClick = { slotIdx = i },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = slots.size),
                            label = { Text(slot.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = name.isNotBlank() &&
                            (kcal.toDoubleOrNull() ?: 0.0) > 0.0 &&
                            (grams.toDoubleOrNull() ?: 0.0) > 0.0,
                        colors = ButtonDefaults.buttonColors(),
                        onClick = {
                            onConfirm(name.trim(), kcal.toDouble(), grams.toDouble(), slots[slotIdx])
                        }
                    ) { Text("Log") }
                }
            }
        }
    }
}
