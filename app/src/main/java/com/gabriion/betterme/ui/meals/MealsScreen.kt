package com.gabriion.betterme.ui.meals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gabriion.betterme.domain.model.Meal
import com.gabriion.betterme.ui.meals.components.DayCard
import com.gabriion.betterme.ui.meals.components.IngredientChips
import com.gabriion.betterme.ui.meals.components.ShoppingListSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealsScreen(viewModel: MealsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    var openedMeal by remember { mutableStateOf<Meal?>(null) }
    var showShopping by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Plan your meals",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Pick ingredients you have on hand — we'll build a balanced plan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        IngredientChips(
            ingredients = state.ingredients,
            selected = state.selected,
            onToggle = viewModel::toggleIngredient,
            wrap = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlanSpan.entries.forEach { span ->
                FilterChip(
                    selected = state.span == span,
                    onClick = { viewModel.setSpan(span) },
                    label = { Text(span.label) },
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.selected.isNotEmpty()) {
                OutlinedButton(onClick = viewModel::clearSelection) { Text("Clear") }
            }
        }

        FilledTonalButton(
            onClick = viewModel::generatePlan,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
        ) {
            Text(if (state.plan == null) "Generate plan" else "Regenerate plan")
        }

        val plan = state.plan
        if (plan == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Tap “Generate plan” to see your meals.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(plan.days, key = { it.dayIndex }) { day ->
                    DayCard(
                        day = day,
                        title = dayTitle(day.dayIndex, state.span),
                        onMealClick = { openedMeal = it },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            FilledTonalButton(
                onClick = { showShopping = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Shopping list (${state.shoppingList.size})")
            }
        }
    }

    openedMeal?.let { meal ->
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { openedMeal = null },
            sheetState = sheet,
        ) {
            MealDetailSheet(meal)
        }
    }

    if (showShopping) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showShopping = false },
            sheetState = sheet,
        ) {
            ShoppingListSheet(items = state.shoppingList)
        }
    }
}

@Composable
private fun MealDetailSheet(meal: Meal) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(meal.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "${meal.slot.label} • ${meal.kcal} kcal",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("Ingredients", style = MaterialTheme.typography.titleSmall)
        meal.ingredients.forEach { ing ->
            Text("• ${ing.replaceFirstChar { it.titlecase() }}", style = MaterialTheme.typography.bodyMedium)
        }
        Text("Macros", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Protein: ${meal.proteinG} g\nCarbs: ${meal.carbsG} g\nFat: ${meal.fatG} g",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
    }
}

private fun dayTitle(index: Int, span: PlanSpan): String = when (span) {
    PlanSpan.TODAY -> "Today"
    PlanSpan.WEEK -> "Day ${index + 1}"
}
