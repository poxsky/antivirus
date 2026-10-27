package com.shieldscan.ui.setup

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SetupScreen(vm: SetupViewModel = viewModel()) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()

    // Refresh on resume
    LaunchedEffect(Unit) { vm.refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Setup & Permissions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Enable for full protection. Everything still works without them, but fewer insights.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        SetupCard(
            title = "1. Usage Access",
            desc = "Lets ShieldScan see foreground/background activity to detect ghost apps & night uploads.\nSettings → Special Access → Usage Access → ShieldScan → Allow",
            granted = state.hasUsageAccess,
            onGrant = {
                try { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } catch (_: Exception) {}
            }
        )
        Spacer(Modifier.height(12.dp))
        SetupCard(
            title = "2. Notifications",
            desc = "For morning scan summary & new-app alerts.",
            granted = state.hasNotificationPerm,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try { ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName) }) } catch (_: Exception) {}
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        SetupCard(
            title = "3. Battery — Unrestricted",
            desc = "Prevents Doze from delaying 7 AM scans.\nSettings → Battery → ShieldScan → Unrestricted",
            granted = state.isBatteryUnrestricted,
            onGrant = {
                try {
                    val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                    ctx.startActivity(i)
                } catch (_: Exception) {
                    try { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (_: Exception) {}
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        SetupCard(
            title = "4. Accessibility (optional)",
            desc = "Only if you want ShieldScan to audit which apps abuse Accessibility (for RAT detection). You review in system settings.",
            granted = state.hasAccessibility,
            onGrant = { try { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (_: Exception) {} }
        )

        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Text("Privacy note", fontWeight = FontWeight.SemiBold)
                Text("ShieldScan runs 100% on-device. No APK upload. Optional VirusTotal check sends only SHA-256 hash with your consent (add key in local.properties).", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top=6.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { vm.refresh() }, modifier = Modifier.fillMaxWidth()) { Text("Refresh status") }
        Spacer(Modifier.height(8.dp))
        Text("Package: ${ctx.packageName} • v0.4.0-alpha", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun SetupCard(title: String, desc: String, granted: Boolean, onGrant: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (granted) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp)); Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f)); Surface(color = if (granted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) { Text(if (granted) "Granted" else "Needed", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall) }
            }
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top=8.dp))
            Spacer(Modifier.height(10.dp))
            Button(onClick = onGrant, enabled = !granted, modifier = Modifier.fillMaxWidth()) { Text(if (granted) "✓ Enabled" else "Enable") }
        }
    }
}
