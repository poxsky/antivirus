package com.shieldscan.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shieldscan.ShieldScanApp
import com.shieldscan.data.db.ScanHistoryEntity
import com.shieldscan.scanner.ScanEngine
import com.shieldscan.service.NotificationHelper

class MorningScanWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            val engine = ScanEngine(applicationContext)
            val result = engine.fullScan()
            val db = (applicationContext as ShieldScanApp).db

            // Upsert apps
            db.appDao().upsertAll(result.entities)
            // Clean up removed apps - need current list
            val current = result.entities.map { it.packageName }
            // Avoid pruning if scan seems incomplete (e.g., permission denied)
            if (current.size > 10) db.appDao().pruneMissing(current)

            // History
            db.historyDao().insert(
                ScanHistoryEntity(
                    scannedAt = System.currentTimeMillis(),
                    totalApps = result.total,
                    criticalCount = result.critical,
                    riskyCount = result.risky,
                    sideloadedCount = result.sideloaded,
                    hiddenCount = result.hidden,
                    durationMs = result.durationMs
                )
            )

            // Notification
            NotificationHelper.showMorningSummary(
                applicationContext, result.total, result.risky, result.critical, result.sideloaded
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
