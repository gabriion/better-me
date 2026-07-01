package com.gabriion.betterme.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Settings screen — the Health Connect permission entry-point in V1.
 *
 * When the platform has Health Connect available, we launch the standard
 * request-permissions contract that shows the system UI. When Health Connect
 * is not installed, we deep-link to the Play Store listing.
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val requestPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ -> viewModel.refresh() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(8.dp)
            )
        }
        item {
            HealthConnectCard(
                available = state.healthConnectAvailable,
                ready = state.healthConnectReady,
                granted = state.grantedCount,
                total = state.totalPermissionsCount,
                onConnect = {
                    if (state.healthConnectAvailable) {
                        requestPermissions.launch(viewModel.requiredPermissions)
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("market://details?id=${viewModel.providerPackage}")
                            setPackage("com.android.vending")
                        }
                        runCatching { context.startActivity(intent) }
                    }
                },
                onManage = {
                    // Launch the system Health Connect app so the user can revoke/adjust.
                    val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
                    runCatching { context.startActivity(intent) }
                }
            )
        }
        item { AboutCard() }
    }
}

@Composable
private fun HealthConnectCard(
    available: Boolean,
    ready: Boolean,
    granted: Int,
    total: Int,
    onConnect: () -> Unit,
    onManage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.HealthAndSafety,
                    contentDescription = null,
                    tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text(
                    "Health data",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    !available -> "Health Connect is not installed on this device. Install it from the Play Store to let Better Me read biometrics from your wearable app (Garmin, Samsung Health, Fitbit, Google Fit, and others)."
                    ready -> "Connected. $granted of $total permissions granted."
                    granted > 0 -> "Partially connected. $granted of $total permissions granted — tap below to grant the rest."
                    else -> "Not connected. Grant read access so tips can be personalised to your sleep, steps, heart rate and HRV."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConnect) {
                    Text(
                        when {
                            !available -> "Install Health Connect"
                            ready -> "Update permissions"
                            else -> "Connect health data"
                        }
                    )
                }
                if (available && (ready || granted > 0)) {
                    OutlinedButton(onClick = onManage) { Text("Manage") }
                }
            }
        }
    }
}

@Composable
private fun AboutCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text("About", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Better Me v1.0 — a quiet companion for mindfulness, movement, meals, and the goals that matter to you. All data stays on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
