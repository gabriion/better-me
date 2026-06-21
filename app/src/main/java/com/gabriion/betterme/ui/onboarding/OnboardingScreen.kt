package com.gabriion.betterme.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    val isValid = name.trim().length in 1..32

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Welcome to Better Me",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "A quiet companion for your day — mindfulness, movement, meals, and the goals that matter to you.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(40.dp))
            Text(
                text = "What should I call you?",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(32) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("First name") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                )
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    viewModel.completeOnboarding(name.trim())
                    onComplete()
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Get started", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
