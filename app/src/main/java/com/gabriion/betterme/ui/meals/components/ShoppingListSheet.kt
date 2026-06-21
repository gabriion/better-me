package com.gabriion.betterme.ui.meals.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gabriion.betterme.domain.meals.ShoppingItem

@Composable
fun ShoppingListSheet(items: List<ShoppingItem>) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Shopping list", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = {
                val text = buildShareText(items)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Shopping list")
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                val chooser = Intent.createChooser(send, "Share shopping list")
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(chooser)
            }) {
                Icon(Icons.Filled.Share, contentDescription = "Share")
            }
        }

        if (items.isEmpty()) {
            Text(
                text = "Generate a plan first to populate your shopping list.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(items, key = { it.ingredient }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.ingredient.replaceFirstChar { it.titlecase() },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            if (item.mealNames.isNotEmpty()) {
                                Text(
                                    text = item.mealNames.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = "×${item.count}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private fun buildShareText(items: List<ShoppingItem>): String = buildString {
    appendLine("Shopping list")
    appendLine()
    for (i in items) {
        append("• ")
        append(i.ingredient.replaceFirstChar { it.titlecase() })
        append(" ×")
        append(i.count)
        appendLine()
    }
}
