package com.shieldscan.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import com.shieldscan.ShieldScanApp
import com.shieldscan.scanner.ScanEngine
import com.shieldscan.data.db.AlertEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class InstallWatchService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val pkg = intent.data?.schemeSpecificPart ?: return
            if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
                val isReplace = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                // For ADDED, EXTRA_REPLACING is false for truly new
                if (action == Intent.ACTION_PACKAGE_ADDED && isReplace) return
                handleNewPackage(pkg)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NotificationHelper.ID_SERVICE, NotificationHelper.foregroundNotification(this))
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun handleNewPackage(pkg: String) {
        scope.launch {
            try {
                val engine = ScanEngine(this@InstallWatchService)
                val scanned = engine.fullScan() // lightweight alternative: scanSingle, but full ensures DB consistent
                val entity = scanned.entities.find { it.packageName == pkg } ?: return@launch
                val db = (application as ShieldScanApp).db

                // Persist
                db.appDao().upsert(entity)

                // Alert if risky
                if (entity.riskScore >= 31) {
                    db.alertDao().insert(
                        AlertEntity(
                            createdAt = System.currentTimeMillis(),
                            type = if (entity.riskScore >= 61) "RISK_CRITICAL" else "NEW_APP",
                            packageName = pkg,
                            title = "New app: ${entity.appName}",
                            message = "Risk ${entity.riskScore}/100 • ${entity.installerLabel} • Tap to review",
                            riskScore = entity.riskScore
                        )
                    )
                    NotificationHelper.showNewAppAlert(
                        this@InstallWatchService,
                        entity.appName, pkg, entity.riskScore, entity.isSideloaded
                    )
                }
            } catch (_: Exception) {}
        }
    }
}
