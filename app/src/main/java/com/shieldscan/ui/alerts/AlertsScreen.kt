package com.shieldscan.ui.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shieldscan.data.db.AlertEntity
import com.shieldscan.util.FormatUtil

@Composable
fun AlertsScreen(onOpenApp: (String)->Unit, vm: AlertsViewModel = viewModel()) {
    val alerts by vm.alerts.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Alerts & History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("${alerts.size} events", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = { vm.markAllRead() }) { Text("Mark all read") }
        }
        Spacer(Modifier.height(12.dp))
        if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No alerts yet — run a scan or install a test app.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(alerts) { alert -> AlertCard(alert, onOpenApp) }
            }
        }
    }
}

@Composable private fun AlertCard(a: AlertEntity, onOpen: (String)->Unit) {
    val color = when { a.riskScore >= 86 -> MaterialTheme.colorScheme.error; a.riskScore >= 61 -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.primary }
    Card(shape = RoundedCornerShape(14.dp), onClick = { onOpen(a.packageName) }) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color.copy(alpha=0.15f), shape = RoundedCornerShape(8.dp)) { Text("${a.riskScore}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
                Spacer(Modifier.width(8.dp)); Text(a.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            }
            Text(a.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top=6.dp))
            Row(Modifier.padding(top=6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(a.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(8.dp)); Text(FormatUtil.shortTime(a.createdAt), style = MaterialTheme.typography.labelSmall)
                if (!a.isRead) { Spacer(Modifier.weight(1f)); Badge { Text("NEW") } }
            }
        }
    }
}
