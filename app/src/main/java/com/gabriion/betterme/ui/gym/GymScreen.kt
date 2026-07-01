package com.gabriion.betterme.ui.gym

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gabriion.betterme.core.db.WorkoutSetEntity
import com.gabriion.betterme.domain.gym.BuiltWorkoutDay
import com.gabriion.betterme.domain.model.Exercise
import com.gabriion.betterme.ui.gym.components.ExerciseDetailSheet
import com.gabriion.betterme.ui.gym.components.ExerciseRow
import com.gabriion.betterme.ui.gym.components.LottiePreview
import com.gabriion.betterme.ui.gym.components.PlanBuilderSheet
import com.gabriion.betterme.ui.gym.components.WorkoutHistoryRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class GymTab(val label: String) {
    BROWSE("Browse"),
    PLAN("My Plan"),
    HISTORY("History"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymScreen(viewModel: GymViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableStateOf(GymTab.BROWSE) }
    var openedExercise by remember { mutableStateOf<Exercise?>(null) }
    var showPlanBuilder by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab.ordinal) {
            GymTab.entries.forEach { t ->
                Tab(
                    selected = tab == t,
                    onClick = { tab = t },
                    text = { Text(t.label) },
                )
            }
        }
        when (tab) {
            GymTab.BROWSE -> BrowseTab(
                exercises = state.exercises,
                onExerciseClick = { openedExercise = it },
            )
            GymTab.PLAN -> PlanTab(
                hasPlan = state.plan != null,
                builtDays = state.builtDays,
                onBuildPlan = { showPlanBuilder = true },
            )
            GymTab.HISTORY -> HistoryTab(
                sets = state.recentSets,
                onDelete = viewModel::deleteSet,
            )
        }
    }

    openedExercise?.let { ex ->
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { openedExercise = null },
            sheetState = sheet,
        ) {
            ExerciseDetailSheet(
                exercise = ex,
                onLog = { reps, weight ->
                    viewModel.logSet(ex, reps, weight)
                    openedExercise = null
                },
            )
        }
    }

    if (showPlanBuilder) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPlanBuilder = false },
            sheetState = sheet,
        ) {
            PlanBuilderSheet(
                templates = state.templates,
                initialTemplateId = state.plan?.templateId,
                initialDaysPerWeek = state.plan?.daysPerWeek ?: 3,
                initialLevel = state.plan?.level ?: "intermediate",
                initialTargetGroups = state.plan
                    ?.targetGroupsCsv
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.toSet()
                    ?: setOf("chest", "back", "shoulders", "legs", "arms", "core"),
                onSave = { templateId, days, level, groups ->
                    viewModel.savePlan(templateId, days, level, groups)
                    showPlanBuilder = false
                },
            )
        }
    }
}

@Composable
private fun BrowseTab(
    exercises: List<Exercise>,
    onExerciseClick: (Exercise) -> Unit,
) {
    if (exercises.isEmpty()) {
        EmptyState("No exercises bundled yet.")
        return
    }
    val grouped = remember(exercises) {
        exercises.groupBy { it.muscleGroup }.toSortedMap()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        grouped.forEach { (group, items) ->
            item(key = "h-$group") {
                Text(
                    text = group.replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(items, key = { it.id }) { ex ->
                ExerciseRow(exercise = ex, onClick = { onExerciseClick(ex) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun PlanTab(
    hasPlan: Boolean,
    builtDays: List<BuiltWorkoutDay>,
    onBuildPlan: () -> Unit,
) {
    if (!hasPlan) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "No training plan yet",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Pick a template and we'll generate your week.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onBuildPlan) { Text("Build my plan") }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FilledTonalButton(
                onClick = onBuildPlan,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Rebuild plan") }
        }
        items(builtDays.size) { i ->
            DayPlanCard(dayNumber = i + 1, day = builtDays[i])
        }
    }
}

@Composable
private fun DayPlanCard(dayNumber: Int, day: BuiltWorkoutDay) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Day $dayNumber • ${day.dayLabel.replaceFirstChar { it.titlecase() }}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            day.exercises.forEachIndexed { idx, planned ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(planned.exercise.name, style = MaterialTheme.typography.titleSmall)
                        val repsText = if (planned.repsLow == planned.repsHigh)
                            "${planned.repsLow} reps"
                        else
                            "${planned.repsLow}–${planned.repsHigh} reps"
                        Text(
                            text = "${planned.sets} sets × $repsText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LottiePreview(
                        assetPath = planned.exercise.lottie,
                        size = 48.dp,
                        muscleGroup = planned.exercise.muscleGroup,
                        pattern = planned.exercise.pattern,
                    )
                }
                if (idx < day.exercises.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTab(
    sets: List<WorkoutSetEntity>,
    onDelete: (Long) -> Unit,
) {
    if (sets.isEmpty()) {
        EmptyState("No sets logged yet. Tap an exercise to log one.")
        return
    }
    val grouped = remember(sets) {
        sets.groupBy { dayKey(it.date) }
            .toSortedMap(compareByDescending { it })
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        grouped.forEach { (key, daySets) ->
            item(key = "dh-$key") {
                Text(
                    text = formatDayHeader(daySets.first().date),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(daySets, key = { it.id }) { set ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { v ->
                        if (v == SwipeToDismissBoxValue.EndToStart || v == SwipeToDismissBoxValue.StartToEnd) {
                            onDelete(set.id)
                            true
                        } else false
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Surface(color = MaterialTheme.colorScheme.errorContainer) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text(
                                    "Delete",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    },
                ) {
                    Surface(color = Color.Transparent) {
                        WorkoutHistoryRow(set = set)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun dayKey(epochMs: Long): Long {
    val day = 24L * 60L * 60L * 1000L
    return epochMs / day
}

private val historyFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM d").withZone(ZoneId.systemDefault())

private fun formatDayHeader(epochMs: Long): String =
    historyFormatter.format(Instant.ofEpochMilli(epochMs))
