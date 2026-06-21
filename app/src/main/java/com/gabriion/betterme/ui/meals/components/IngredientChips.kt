package com.gabriion.betterme.ui.meals.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IngredientChips(
    ingredients: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    wrap: Boolean = true,
) {
    if (wrap) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (item in ingredients) {
                IngredientFilterChip(item, item in selected) { onToggle(item) }
            }
        }
    } else {
        Row(
            modifier = modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (item in ingredients) {
                IngredientFilterChip(item, item in selected) { onToggle(item) }
            }
        }
    }
}

@Composable
private fun IngredientFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label.replaceFirstChar { it.titlecase() }) },
        colors = FilterChipDefaults.filterChipColors(),
    )
}
