package com.gabriion.betterme.ui.gym.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriion.betterme.domain.model.WorkoutTemplate

private val ALL_GROUPS = listOf("chest", "back", "shoulders", "legs", "arms", "core")
private val LEVELS = listOf("beginner", "intermediate", "advanced")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlanBuilderSheet(
    templates: List<WorkoutTemplate>,
    initialTemplateId: String? = null,
    initialDaysPerWeek: Int = 3,
    initialLevel: String = "intermediate",
    initialTargetGroups: Set<String> = ALL_GROUPS.toSet(),
    onSave: (templateId: String, daysPerWeek: Int, level: String, targetGroups: Set<String>) -> Unit,
) {
    var selectedTemplateId by remember {
        mutableStateOf(initialTemplateId ?: templates.firstOrNull()?.id.orEmpty())
    }
    var days by remember { mutableStateOf(initialDaysPerWeek.coerceIn(2, 6).toFloat()) }
    var level by remember { mutableStateOf(initialLevel) }
    var groups by remember { mutableStateOf(initialTargetGroups) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Build your plan", style = MaterialTheme.typography.headlineSmall)

        Text("Template", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            templates.forEach { t ->
                FilterChip(
                    selected = selectedTemplateId == t.id,
                    onClick = { selectedTemplateId = t.id },
                    label = { Text(t.name) },
                )
            }
        }

        Text("Days per week: ${days.toInt()}", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = days,
            onValueChange = { days = it },
            valueRange = 2f..6f,
            steps = 3, // 2,3,4,5,6
        )

        Text("Level", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            LEVELS.forEachIndexed { index, l ->
                SegmentedButton(
                    selected = level == l,
                    onClick = { level = l },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = LEVELS.size),
                ) {
                    Text(l.replaceFirstChar { it.titlecase() })
                }
            }
        }

        Text("Target muscle groups", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ALL_GROUPS.forEach { g ->
                FilterChip(
                    selected = g in groups,
                    onClick = {
                        groups = if (g in groups) groups - g else groups + g
                    },
                    label = { Text(g.replaceFirstChar { it.titlecase() }) },
                )
            }
        }

        FilledTonalButton(
            onClick = {
                if (selectedTemplateId.isNotBlank()) {
                    onSave(selectedTemplateId, days.toInt(), level, groups)
                }
            },
            enabled = selectedTemplateId.isNotBlank() && groups.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save plan") }

        Spacer(Modifier.height(8.dp))
    }
}

// Re-export for callers that want to render group chips without depending on internals.
fun planBuilderGroupOptions(): List<String> = ALL_GROUPS
