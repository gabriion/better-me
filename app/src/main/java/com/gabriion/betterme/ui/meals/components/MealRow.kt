package com.gabriion.betterme.ui.meals.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriion.betterme.domain.model.Meal
import com.gabriion.betterme.domain.model.Slot

@Composable
fun MealRow(
    slot: Slot,
    meal: Meal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(96.dp)) {
            Text(
                text = slot.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(meal.name, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${meal.proteinG}P / ${meal.carbsG}C / ${meal.fatG}F",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${meal.kcal} kcal",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
