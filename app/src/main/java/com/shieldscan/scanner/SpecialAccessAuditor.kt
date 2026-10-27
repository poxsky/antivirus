package com.shieldscan.scanner

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

data class SpecialAccessState(
    val hasAccessibility: Boolean = false,
    val accessibilityServices: List<String> = emptyList(),
    val isDeviceAdmin: Boolean = false,
    val hasNotificationListener: Boolean = false,
    val hasOverlay: Boolean = false,
    val canInstallUnknownApps: Boolean = false
)

class SpecialAccessAuditor(private val ctx: Context) {

    private val pm = ctx.packageManager

    fun auditForPackage(packageName: String, uid: Int = -1): SpecialAccessState {
        val resolvedUid = if (uid == -1) getUid(packageName) else uid
        return SpecialAccessState(
            hasAccessibility = isAccessibilityEnabledFor(packageName),
            accessibilityServices = getEnabledAccessibilityServices(),
            isDeviceAdmin = isDeviceAdmin(packageName),
            hasNotificationListener = hasNotificationListener(packageName),
            hasOverlay = hasOverlay(packageName, resolvedUid),
            canInstallUnknownApps = canInstallUnknownApps(packageName, resolvedUid)
        )
    }

    fun isAccessibilityEnabledFor(pkg: String): Boolean {
        val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.contains(pkg)
    }

    fun getEnabledAccessibilityServices(): List<String> {
        val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return emptyList()
        return enabled.split(":").filter { it.isNotBlank() }
    }

    fun isDeviceAdmin(pkg: String): Boolean {
        return try {
            val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.activeAdmins?.any { it.packageName == pkg } ?: false
        } catch (_: Exception) { false }
    }

    fun hasNotificationListener(pkg: String): Boolean {
        return try {
            NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(pkg)
        } catch (_: Exception) { false }
    }

    fun hasOverlay(pkg: String, uid: Int): Boolean {
        return try {
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, uid, pkg)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, uid, pkg)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) { false }
    }

    fun canInstallUnknownApps(pkg: String, uid: Int): Boolean {
        return try {
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val op = "android:request_install_packages"
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(op, uid, pkg)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(op, uid, pkg)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) { false }
    }

    private fun getUid(pkg: String): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)).applicationInfo?.uid ?: -1
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0).applicationInfo?.uid ?: -1
            }
        } catch (_: Exception) { -1 }
    }
}
