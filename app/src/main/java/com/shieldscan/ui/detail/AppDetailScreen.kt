package com.shieldscan.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shieldscan.util.FormatUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(packageName: String, onBack: () -> Unit, vm: AppDetailViewModel = viewModel()) {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf<AppDetailState?>(null) }

    LaunchedEffect(packageName) { state = vm.load(packageName) }

    Scaffold(topBar = { TopAppBar(title = { Text(state?.entity?.appName ?: packageName, maxLines=1) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        if (state == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val s = state!!
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { ScoreHeader(s.entity.riskScore, s.entity.riskLevel) }

            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DetailRow("Package", s.entity.packageName)
                        DetailRow("Version", "${s.entity.versionName} (${s.entity.versionCode})")
                        DetailRow("Installer", s.entity.installerLabel + if(s.entity.isSideloaded) " ⚠️ Sideloaded" else "")
                        DetailRow("System app", if(s.entity.isSystemApp) "Yes" else "No")
                        DetailRow("Hidden icon", if(s.entity.isHidden) "Yes — suspicious" else "No")
                        DetailRow("APK path", s.entity.apkPath)
                        DetailRow("APK size", FormatUtil.formatBytes(s.entity.apkSize))
                        DetailRow("SHA-256", s.entity.sha256.take(32)+"…")
                        DetailRow("Signer SHA-256", s.entity.signerSha256?.take(24)?.plus("…") ?: "—")
                        DetailRow("First install", FormatUtil.formatDate(s.entity.firstInstallTime))
                        DetailRow("Last update", FormatUtil.formatDate(s.entity.lastUpdateTime))
                        DetailRow("Target SDK", "${s.targetSdk} / Min ${s.minSdk}")
                    }
                }
            }

            if (s.reasons.isNotEmpty()) {
                item {
                    Text("Why flagged", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.reasons.forEach { r ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Text(r, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            item {
                Text("Permissions (${s.granted.size} granted / ${s.requested.size} requested)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.requested.forEach { perm ->
                        val granted = perm in s.granted
                        val isDangerous = perm.contains("SMS") || perm.contains("CONTACT") || perm.contains("LOCATION") || perm.contains("CAMERA") || perm.contains("RECORD")
                        val suspicious = perm !in s.expected
                        Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = when {
                            granted && suspicious -> MaterialTheme.colorScheme.errorContainer
                            granted -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        })) {
                            Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(perm.substringAfterLast("."), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    if (suspicious && granted) Text("Not needed for this category ⚠️", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                                Text(if (granted) "GRANTED" else "REQUESTED", style = MaterialTheme.typography.labelSmall, color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (s.embeddedUrls.isNotEmpty() || s.embeddedIps.isNotEmpty()) {
                item {
                    Text("Embedded network indicators", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (s.embeddedUrls.isNotEmpty()) {
                        Text("URLs:", style = MaterialTheme.typography.labelMedium); s.embeddedUrls.take(10).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                    }
                    if (s.embeddedIps.isNotEmpty()) {
                        Text("IPs:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top=6.dp)); s.embeddedIps.take(10).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        try { ctx.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))) } catch (_: Exception) {}
                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Uninstall")
                    }
                    OutlinedButton(onClick = {
                        try { ctx.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) } catch (_: Exception) {}
                    }, modifier = Modifier.weight(1f)) { Text("App Info") }
                }
                if (s.isSystemApp) Text("System apps may require root/ADB to uninstall.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top=6.dp))
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable private fun ScoreHeader(score: Int, level: String) {
    val color = when(level){ "CRITICAL"->MaterialTheme.colorScheme.error; "RISKY"->MaterialTheme.colorScheme.tertiary; "REVIEW"->MaterialTheme.colorScheme.secondary; else->MaterialTheme.colorScheme.primary }
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha=0.12f))) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = color, shape = RoundedCornerShape(12.dp)) { Text("$score", modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.width(12.dp)); Column { Text(level, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.titleMedium); Text("$score/100 — ${when(level){"CRITICAL"->"Immediate action"; "RISKY"->"Review needed"; "REVIEW"->"Check permissions"; else->"Safe"}}", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
@Composable private fun DetailRow(k: String, v: String) { Row(Modifier.fillMaxWidth()) { Text("$k: ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold); Text(v, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)) } }
