package com.gabriion.betterme.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val requestPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.refresh()
        val msg = if (granted.isNotEmpty()) "${granted.size} permission(s) granted"
                  else "No permissions granted — you can try again anytime."
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

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
                        runCatching {
                            requestPermissions.launch(viewModel.requiredPermissions)
                        }.onFailure {
                            Toast.makeText(
                                context,
                                "Couldn't open Health Connect: ${it.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        // Try Play Store app first, fall back to web URL.
                        val pkg = viewModel.providerPackage
                        val marketUri = Uri.parse("market://details?id=$pkg")
                        val marketIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        val webIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        try {
                            context.startActivity(marketIntent)
                        } catch (_: ActivityNotFoundException) {
                            try {
                                context.startActivity(webIntent)
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    "Open Play Store and search for 'Health Connect'.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                },
                onManage = {
                    // Open the system Health Connect app so the user can revoke/adjust.
                    val settingsIntent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val launchApp = context.packageManager
                        .getLaunchIntentForPackage(viewModel.providerPackage)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(settingsIntent)
                    } catch (_: ActivityNotFoundException) {
                        if (launchApp != null) {
                            try { context.startActivity(launchApp) } catch (_: Exception) {
                                Toast.makeText(context, "Couldn't open Health Connect.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Health Connect app not found.", Toast.LENGTH_SHORT).show()
                        }
                    }
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
                    !available -> "Health Connect is not installed on this device. Tap below to install it from the Play Store — Better Me reads biometrics from your wearable app (Garmin, Samsung Health, Fitbit, Google Fit, and others) via Health Connect."
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
                "Better Me v1.1 — a quiet companion for mindfulness, movement, meals, and the goals that matter to you. All data stays on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
