package com.gabriion.betterme.ui.goals

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gabriion.betterme.data.repository.GoalTemplate
import com.gabriion.betterme.domain.model.GoalProgressEvent
import com.gabriion.betterme.domain.model.GoalType
import com.gabriion.betterme.ui.goals.components.GoalCard
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: GoalsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var selectedGoalId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add goal")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.goals.isEmpty() && !state.loading) {
                EmptyState(onCreate = { showAdd = true })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item {
                        Text(
                            "Your goals",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
                        )
                    }
                    items(state.goals, key = { it.goal.id }) { item ->
                        GoalCard(
                            item = item,
                            onClick = { selectedGoalId = item.goal.id }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAdd) {
        AddGoalSheet(
            templates = state.templates,
            onDismiss = { showAdd = false },
            onCreate = { title, type, target, unit, sport ->
                viewModel.addGoal(title, type, target, unit, null, sport)
                showAdd = false
            },
            onPickTemplate = { tpl ->
                viewModel.addFromTemplate(tpl)
                showAdd = false
            }
        )
    }

    selectedGoalId?.let { goalId ->
        val item = state.goals.firstOrNull { it.goal.id == goalId }
        if (item != null) {
            GoalDetailSheet(
                item = item,
                eventsFlow = remember(goalId) { viewModel.eventsFor(goalId) },
                onDismiss = { selectedGoalId = null },
                onLog = { amount, note -> viewModel.logProgress(goalId, amount, note) },
                onArchive = {
                    viewModel.archive(goalId)
                    selectedGoalId = null
                }
            )
        } else {
            selectedGoalId = null
        }
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌱", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "No goals yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Plant a small intention today and watch it grow.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreate) { Text("Create your first goal") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalSheet(
    templates: List<GoalTemplate>,
    onDismiss: () -> Unit,
    onCreate: (String, GoalType, Double, String, Boolean) -> Unit,
    onPickTemplate: (GoalTemplate) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(GoalType.COUNT) }
    var sport by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    val chipsScroll = rememberScrollState()
    val pageScroll = rememberScrollState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(pageScroll)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("New goal", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            if (templates.isNotEmpty()) {
                Text("Templates", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(chipsScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { tpl ->
                        AssistChip(
                            onClick = { onPickTemplate(tpl) },
                            label = { Text(tpl.title) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = type.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                androidx.compose.material3.ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    GoalType.values().forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.name) },
                            onClick = {
                                type = t
                                typeExpanded = false
                                if (unit.isBlank()) unit = defaultUnitFor(t)
                                sport = t == GoalType.DISTANCE_KM || t == GoalType.DURATION_MIN
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Target") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = sport, onCheckedChange = { sport = it })
                Spacer(Modifier.width(8.dp))
                Text("Auto-receive Garmin events", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = title.isNotBlank() && (target.toDoubleOrNull() ?: 0.0) > 0.0 && unit.isNotBlank(),
                    onClick = {
                        onCreate(title.trim(), type, target.toDouble(), unit.trim(), sport)
                    }
                ) { Text("Create") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDetailSheet(
    item: GoalWithProgress,
    eventsFlow: Flow<List<GoalProgressEvent>>,
    onDismiss: () -> Unit,
    onLog: (Double, String?) -> Unit,
    onArchive: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val events by eventsFlow.collectAsState(initial = emptyList())
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(item.goal.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "${"%.0f".format(item.summary.percent * 100)}% • ${"%.1f".format(item.summary.remaining)} ${item.goal.unit} to go",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            item.summary.projectedCompletion?.let {
                Text(
                    "Projected: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.weight(1.4f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onArchive) { Text("Archive") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                    onClick = {
                        onLog(amount.toDouble(), note.ifBlank { null })
                        amount = ""
                        note = ""
                    }
                ) { Text("Log progress") }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text("History", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            if (events.isEmpty()) {
                Text(
                    "No progress logged yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    items(events, key = { it.id }) { ev ->
                        EventRow(ev, item.goal.unit)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EventRow(event: GoalProgressEvent, unit: String) {
    val df = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "+${"%.1f".format(event.amount)} $unit" + (event.note?.let { " — $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                df.format(Date(event.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (event.source != "manual") {
            AssistChip(onClick = {}, label = { Text(event.source) })
        }
    }
}

private fun defaultUnitFor(type: GoalType): String = when (type) {
    GoalType.COUNT -> "items"
    GoalType.DISTANCE_KM -> "km"
    GoalType.DURATION_MIN -> "min"
    GoalType.FREQUENCY_DAYS -> "days"
    GoalType.WEIGHT_KG -> "kg"
}
