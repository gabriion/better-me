package com.gabriion.betterme.ui.tips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DailyTipsScreen() {
    val placeholderTips = listOf(
        "Connect your Garmin in Settings to receive personalised tips.",
        "Two minutes of slow breathing settles the nervous system.",
        "A 15-minute walk between meetings improves afternoon focus."
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Daily Tips", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(8.dp))
        }
        items(placeholderTips) { tip ->
            Card {
                Text(tip, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
