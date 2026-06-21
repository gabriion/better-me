package com.gabriion.betterme.ui.meals.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriion.betterme.domain.meals.DayPlan
import com.gabriion.betterme.domain.model.Meal

@Composable
fun DayCard(
    day: DayPlan,
    title: String,
    onMealClick: (Meal) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${day.totalKcal} kcal",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            day.items.forEachIndexed { i, item ->
                MealRow(
                    slot = item.slot,
                    meal = item.meal,
                    onClick = { onMealClick(item.meal) },
                )
                if (i < day.items.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
