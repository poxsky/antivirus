package com.shieldscan.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
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
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val riskScore: Int,
    val riskLevel: String,
    val grantedPermissionsJson: String,
    val reasonsJson: String,
    val scannedAt: Long
)

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scannedAt: Long,
    val totalApps: Int,
    val criticalCount: Int,
    val riskyCount: Int,
    val sideloadedCount: Int,
    val hiddenCount: Int,
    val durationMs: Long
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val type: String, // NEW_APP, RISK_CRITICAL, ANOMALY
    val packageName: String,
    val title: String,
    val message: String,
    val riskScore: Int,
    val isRead: Boolean = false
)
