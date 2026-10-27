package com.shieldscan.ui.dashboard

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shieldscan.data.RiskLevel
import com.shieldscan.service.InstallWatchService
import com.shieldscan.workers.ScanScheduler

@Composable
fun DashboardScreen(onOpenApp: (String) -> Unit, onOpenList: () -> Unit, vm: DashboardViewModel = viewModel()) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        try {
            val svc = Intent(ctx, InstallWatchService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(svc) else ctx.startService(svc)
        } catch (_: Exception) {}
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("ShieldScan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("On-device auditor • privacy-first", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatChip("Total", "${state.totalApps}", MaterialTheme.colorScheme.primary)
                        StatChip("Critical", "${state.criticalCount}", MaterialTheme.colorScheme.error)
                        StatChip("Hidden", "${state.hiddenCount}", MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }

        item {
            SecurityScoreCard(score = state.avgScore, level = state.overallLevel)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { vm.refresh(); ScanScheduler.runNow(ctx) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Scan Now")
                }
                OutlinedButton(onClick = onOpenList, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Security, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("View All Apps")
                }
            }
            Text("Last scan: ${state.lastScanLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top=8.dp))
        }

        item {
            SectionTitle("Highlights")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HighlightRow("📦 Sideloaded", "${state.sideloadedCount} apps not from Play Store")
                HighlightRow("🫥 Hidden apps", "${state.hiddenCount} without launcher icon")
                HighlightRow("🔔 Alerts", "${state.alertCount} pending")
                HighlightRow("🕒 Scan history", "${state.historyCount} past scans")
            }
        }

        if (state.topRisky.isNotEmpty()) {
            item {
                SectionTitle("Top Risky Apps")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.topRisky.forEach { app ->
                        RiskAppRow(app.packageName, app.appName, app.riskScore, app.riskLevel) { onOpenApp(app.packageName) }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("How scoring works", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Every app gets 0-100 based on permissions, hidden status, sideload source, and combo patterns (e.g., SMS + Overlay = banking trojan). Tap any app for plain-English reasons.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top=6.dp))
                }
            }
        }
    }
}

@Composable private fun StatChip(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Surface(color = color.copy(alpha=0.12f), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun SecurityScoreCard(score: Int, level: RiskLevel) {
    val color = when(level) { RiskLevel.SAFE -> MaterialTheme.colorScheme.primary; RiskLevel.REVIEW -> MaterialTheme.colorScheme.secondary; RiskLevel.RISKY -> MaterialTheme.colorScheme.tertiary; RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error }
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha=0.10f))) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(progress = { score/100f }, modifier = Modifier.size(56.dp), strokeWidth = 6.dp, color = color, trackColor = color.copy(alpha=0.2f))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Device Safety Score", style = MaterialTheme.typography.labelMedium)
                Text("$score / 100 — ${level.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                Text(when(level){ RiskLevel.SAFE->"All clear — keep updating OS"; RiskLevel.REVIEW->"Review flagged apps"; RiskLevel.RISKY->"Action recommended"; RiskLevel.CRITICAL->"Immediate review needed"}, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable private fun SectionTitle(t: String) { Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
@Composable private fun HighlightRow(title: String, sub: String) {
    Card(shape = RoundedCornerShape(12.dp)) { Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium); Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    } }
}
@Composable private fun RiskAppRow(pkg: String, name: String, score: Int, level: String, onClick: () -> Unit) {
    val color = when(level){ "CRITICAL"->MaterialTheme.colorScheme.error; "RISKY"->MaterialTheme.colorScheme.tertiary; else->MaterialTheme.colorScheme.primary }
    Card(onClick = onClick, shape = RoundedCornerShape(12.dp)) { Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Medium); Text(pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Surface(color = color.copy(alpha=0.15f), shape = RoundedCornerShape(8.dp)) { Text("$score", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = color, fontWeight = FontWeight.Bold) }
    } }
}
