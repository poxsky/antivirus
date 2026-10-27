package com.shieldscan.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shieldscan.ShieldScanApp
import com.shieldscan.data.RiskLevel
import com.shieldscan.scanner.ScanEngine
import com.shieldscan.util.FormatUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DashboardState(
    val totalApps: Int = 0,
    val criticalCount: Int = 0,
    val riskyCount: Int = 0,
    val sideloadedCount: Int = 0,
    val hiddenCount: Int = 0,
    val alertCount: Int = 0,
    val historyCount: Int = 0,
    val avgScore: Int = 78,
    val overallLevel: RiskLevel = RiskLevel.SAFE,
    val lastScanLabel: String = "Never",
    val topRisky: List<TopApp> = emptyList()
)
data class TopApp(val packageName: String, val appName: String, val riskScore: Int, val riskLevel: String)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as ShieldScanApp).db
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            try {
                // If DB empty, scan now
                val count = db.appDao().count()
                if (count == 0) {
                    val engine = ScanEngine(getApplication())
                    val res = engine.fullScan()
                    db.appDao().upsertAll(res.entities)
                    db.historyDao().insert(
                        com.shieldscan.data.db.ScanHistoryEntity(
                            scannedAt = System.currentTimeMillis(),
                            totalApps = res.total,
                            criticalCount = res.critical,
                            riskyCount = res.risky,
                            sideloadedCount = res.sideloaded,
                            hiddenCount = res.hidden,
                            durationMs = res.durationMs
                        )
                    )
                }
                val apps = db.appDao().getAll()
                val history = db.historyDao().getLast()
                val alerts = db.alertDao().getAll()

                val total = apps.size
                val critical = apps.count { it.riskLevel == "CRITICAL" }
                val risky = apps.count { it.riskLevel == "RISKY" }
                val sideloaded = apps.count { it.isSideloaded }
                val hidden = apps.count { it.isHidden }
                val avg = if (apps.isEmpty()) 100 else 100 - (apps.sumOf { it.riskScore } / apps.size).coerceIn(0,100)
                val level = when { critical > 0 -> RiskLevel.CRITICAL; risky >= 3 -> RiskLevel.RISKY; risky >0 -> RiskLevel.REVIEW; else -> RiskLevel.SAFE }

                val top = apps.sortedByDescending { it.riskScore }.take(5).map { TopApp(it.packageName, it.appName, it.riskScore, it.riskLevel) }

                _state.value = DashboardState(
                    totalApps = total, criticalCount = critical, riskyCount = risky,
                    sideloadedCount = sideloaded, hiddenCount = hidden,
                    alertCount = alerts.size, historyCount = if (history==null) 0 else 1,
                    avgScore = avg, overallLevel = level,
                    lastScanLabel = history?.let { FormatUtil.formatDate(it.scannedAt) } ?: FormatUtil.formatDate(System.currentTimeMillis()),
                    topRisky = top
                )
            } catch (_: Exception) {}
        }
    }
}
