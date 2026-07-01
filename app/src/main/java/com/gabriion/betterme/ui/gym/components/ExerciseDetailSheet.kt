package com.gabriion.betterme.ui.gym.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gabriion.betterme.domain.model.Exercise

@Composable
fun ExerciseDetailSheet(
    exercise: Exercise,
    onLog: (reps: Int, weightKg: Double?) -> Unit,
) {
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            LottiePreview(
                assetPath = exercise.lottie,
                size = 140.dp,
                muscleGroup = exercise.muscleGroup,
            )
        }
        Text(exercise.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "${exercise.muscleGroup.replaceFirstChar { it.titlecase() }} • " +
                exercise.equipment.replaceFirstChar { it.titlecase() },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Recommended: ${exercise.setsDefault} sets × " +
                "${exercise.repsLow}–${exercise.repsHigh} reps",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("Log a set", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it.filter { ch -> ch.isDigit() }.take(3) },
                label = { Text("Reps") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = weight,
                onValueChange = {
                    weight = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6)
                },
                label = { Text("Weight (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        FilledTonalButton(
            onClick = {
                val r = reps.toIntOrNull() ?: return@FilledTonalButton
                val w = weight.replace(',', '.').toDoubleOrNull()
                onLog(r, w)
            },
            enabled = reps.toIntOrNull()?.let { it > 0 } == true,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save set")
        }
        Spacer(Modifier.size(8.dp))
    }
}
