package com.gabriion.betterme.ui.calories.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriion.betterme.core.db.FoodCacheEntity

@Composable
fun FoodSearchRow(
    item: FoodCacheEntity,
    onClick: () -> Unit,
    onToggleFavourite: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
            if (!item.brand.isNullOrBlank()) {
                Text(
                    item.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    "${item.kcalPer100g.toInt()} kcal / 100g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "P ${"%.0f".format(item.proteinPer100g)} · C ${"%.0f".format(item.carbsPer100g)} · F ${"%.0f".format(item.fatPer100g)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = { onToggleFavourite(!item.favourite) }) {
            if (item.favourite) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Unfavourite",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(Icons.Outlined.StarBorder, contentDescription = "Favourite")
            }
        }
    }
}
