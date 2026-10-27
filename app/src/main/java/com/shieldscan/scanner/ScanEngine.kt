package com.shieldscan.scanner

import android.content.Context
import android.content.pm.PackageManager
import com.shieldscan.data.RiskLevel
import com.shieldscan.data.db.AppEntity
import com.shieldscan.risk.CategoryExpectations
import com.shieldscan.risk.RiskEngine
import com.google.gson.Gson

class ScanEngine(private val ctx: Context) {
    private val pm: PackageManager = ctx.packageManager
    private val inventory = InventoryScanner(pm)
    private val auditor = SpecialAccessAuditor(ctx)
    private val behaviorMonitor = BehaviorMonitor(ctx)
    private val gson = Gson()

    data class ScanResult(
        val entities: List<AppEntity>,
        val total: Int,
        val critical: Int,
        val risky: Int,
        val sideloaded: Int,
        val hidden: Int,
        val durationMs: Long
    )

    fun fullScan(): ScanResult {
        val t0 = System.currentTimeMillis()
        val apps = inventory.scanAll()
        val entities = apps.map { app ->
            val uid = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0)).applicationInfo?.uid ?: -1
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(app.packageName, 0).applicationInfo?.uid ?: -1
                }
            } catch (_: Exception) { -1 }

            val special = auditor.auditForPackage(app.packageName, uid)
            // Category inference
            val catFromInfo = try {
                val ai = pm.getApplicationInfo(app.packageName, 0)
                CategoryExpectations.fromApplicationInfo(ai)
            } catch (_: Exception) { CategoryExpectations.AppCategory.UNKNOWN }
            val category = if (catFromInfo == CategoryExpectations.AppCategory.UNKNOWN) {
                CategoryExpectations.inferFromPackage(app.packageName, app.appName)
            } else catFromInfo

            val risk = RiskEngine.assess(app, special, category)

            AppEntity(
                packageName = app.packageName,
                appName = app.appName,
                versionName = app.versionName,
                versionCode = app.versionCode,
                apkPath = app.apkPath,
                apkSize = app.apkSize,
                sha256 = app.sha256,
                signerSha256 = app.signerSha256,
                installerLabel = app.installerLabel,
                isSideloaded = app.isSideloaded,
                isSystemApp = app.isSystemApp,
                isHidden = app.isHidden,
                firstInstallTime = app.firstInstallTime,
                lastUpdateTime = app.lastUpdateTime,
                riskScore = risk.score,
                riskLevel = risk.level.name,
                grantedPermissionsJson = gson.toJson(app.grantedPermissions),
                reasonsJson = gson.toJson(risk.reasons),
                scannedAt = System.currentTimeMillis()
            )
        }

        val critical = entities.count { it.riskLevel == RiskLevel.CRITICAL.name }
        val risky = entities.count { it.riskLevel == RiskLevel.RISKY.name }
        val sideloaded = entities.count { it.isSideloaded }
        val hidden = entities.count { it.isHidden }
        val duration = System.currentTimeMillis() - t0

        return ScanResult(entities, entities.size, critical, risky, sideloaded, hidden, duration)
    }
}
