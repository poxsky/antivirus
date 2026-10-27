package com.shieldscan.data

data class ScannedApp(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val versionCode: Long,
    val apkPath: String,
    val apkSize: Long,
    val sha256: String,
    val signerSha256: String?,
    val installerLabel: String,
    val isSideloaded: Boolean,
    val isSystemApp: Boolean,
    val isHidden: Boolean,
    val hasLauncherIcon: Boolean,
    val minSdk: Int,
    val targetSdk: Int,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val requestedPermissions: List<String>,
    val grantedPermissions: List<String>,
    val permissionsRequestedCount: Int,
    val permissionsGrantedCount: Int
)

data class ScanReport(
    val scannedAt: Long = System.currentTimeMillis(),
    val totalApps: Int,
    val riskyCount: Int,
    val criticalCount: Int,
    val sideloadedCount: Int,
    val hiddenCount: Int
)

enum class RiskLevel { SAFE, REVIEW, RISKY, CRITICAL }

data class RiskResult(
    val score: Int,
    val level: RiskLevel,
    val reasons: List<String>,
    val matchedCombos: List<String>
)
