package com.shieldscan.workers

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shieldscan.ShieldScanApp
import com.shieldscan.data.db.AlertEntity
import com.shieldscan.scanner.InventoryScanner
import com.shieldscan.scanner.ScanEngine
import com.shieldscan.service.NotificationHelper

class DiffWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            val pm = applicationContext.packageManager
            // Current installed set via lightweight scan
            val inventory = InventoryScanner(pm)
            val currentApps = inventory.scanAll() // already optimized
            val currentPkgs = currentApps.map { it.packageName }.toSet()

            val db = (applicationContext as ShieldScanApp).db
            val stored = db.appDao().getAll().map { it.packageName }.toSet()

            // New apps = in current but not in DB
            val newPkgs = currentPkgs - stored
            // Updated apps = lastUpdateTime changed - we detect via full entity compare
            // For simplicity, treat any package where DB entry is null as new
            if (newPkgs.isNotEmpty()) {
                // Do a full scan to compute risk for new ones (and refresh all)
                val engine = ScanEngine(applicationContext)
                val result = engine.fullScan()
                db.appDao().upsertAll(result.entities)

                val newEntities = result.entities.filter { it.packageName in newPkgs }
                newEntities.forEach { e ->
                    if (e.riskScore >= 25) {
                        db.alertDao().insert(
                            AlertEntity(
                                createdAt = System.currentTimeMillis(),
                                type = "NEW_APP",
                                packageName = e.packageName,
                                title = "New app detected: ${e.appName}",
                                message = "Risk ${e.riskScore}/100 via diff scan • ${e.installerLabel}",
                                riskScore = e.riskScore
                            )
                        )
                        // Only notify for risky+
                        if (e.riskScore >= 40) {
                            NotificationHelper.showNewAppAlert(
                                applicationContext, e.appName, e.packageName, e.riskScore, e.isSideloaded
                            )
                        }
                    }
                }
            } else {
                // No new, but still check for sideloaded hidden that became risky? no-op
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
