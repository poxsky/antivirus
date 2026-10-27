package com.shieldscan.ui.applist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shieldscan.data.db.AppEntity

@Composable
fun AppListScreen(onOpenApp: (String) -> Unit, vm: AppListViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Filter.ALL) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Installed Apps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("${state.apps.size} apps • tap to inspect", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(value = query, onValueChange = { query = it; vm.filter(query, filter) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search app or package") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter==Filter.ALL, onClick = { filter=Filter.ALL; vm.filter(query, filter) }, label = { Text("All") })
            FilterChip(selected = filter==Filter.RISKY, onClick = { filter=Filter.RISKY; vm.filter(query, filter) }, label = { Text("Risky") })
            FilterChip(selected = filter==Filter.SIDELOADED, onClick = { filter=Filter.SIDELOADED; vm.filter(query, filter) }, label = { Text("Sideloaded") })
            FilterChip(selected = filter==Filter.HIDDEN, onClick = { filter=Filter.HIDDEN; vm.filter(query, filter) }, label = { Text("Hidden") })
            FilterChip(selected = filter==Filter.SYSTEM, onClick = { filter=Filter.SYSTEM; vm.filter(query, filter) }, label = { Text("System") })
        }
        Spacer(Modifier.height(12.dp))

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.filtered) { app -> AppRow(app, onOpenApp) }
            }
        }
    }
}

enum class Filter { ALL, RISKY, SIDELOADED, HIDDEN, SYSTEM }

@Composable
private fun AppRow(app: AppEntity, onOpen: (String)->Unit) {
    val badgeColor = when(app.riskLevel) {
        "CRITICAL" -> MaterialTheme.colorScheme.error
        "RISKY" -> MaterialTheme.colorScheme.tertiary
        "REVIEW" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    Card(onClick = { onOpen(app.packageName) }, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(app.appName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    if (app.isSideloaded) BadgeLabel("SIDELOADED")
                    if (app.isHidden) BadgeLabel("HIDDEN")
                    if (app.isSystemApp) BadgeLabel("SYSTEM")
                }
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text("${app.installerLabel} • v${app.versionName ?: "-"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Surface(color = badgeColor.copy(alpha=0.15f), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${app.riskScore}", fontWeight = FontWeight.Bold, color = badgeColor)
                    Text(app.riskLevel, style = MaterialTheme.typography.labelSmall, color = badgeColor)
                }
            }
        }
    }
}
@Composable private fun BadgeLabel(t: String) { Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) { Text(t, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall) } }
