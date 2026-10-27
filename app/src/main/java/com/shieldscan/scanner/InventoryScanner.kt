package com.shieldscan.scanner

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.shieldscan.data.ScannedApp
import com.shieldscan.util.HashUtil
import com.shieldscan.util.InstallSourceUtil
import java.io.File

class InventoryScanner(private val pm: PackageManager) {

    fun scanAll(): List<ScannedApp> {
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(flags)
        }
        return packages.mapNotNull { pkg -> parsePackage(pkg) }
    }

    fun scanSingle(packageName: String): ScannedApp? {
        return try {
            val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES
            val pkg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, flags)
            }
            parsePackage(pkg)
        } catch (_: Exception) { null }
    }

    private fun parsePackage(pkg: PackageInfo): ScannedApp? {
        val appInfo = pkg.applicationInfo ?: return null
        val label = pm.getApplicationLabel(appInfo)?.toString() ?: pkg.packageName
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

        val apkPath = appInfo.sourceDir ?: ""
        val apkFile = File(apkPath)
        val apkSize = if (apkFile.exists()) apkFile.length() else 0L
        val sha256 = try { HashUtil.sha256(apkFile) } catch (_: Exception) { "" }

        val signerSha256 = try {
            val signers = pkg.signingInfo?.apkContentsSigners
            signers?.firstOrNull()?.let { HashUtil.sha256(it.toByteArray()) }
        } catch (_: Exception) { null }

        val (installerLabel, isSideloaded) = InstallSourceUtil.getInstallerLabel(pm, pkg.packageName)

        val hasLauncher = pm.getLaunchIntentForPackage(pkg.packageName) != null
        val isHidden = !hasLauncher && !isSystem

        val requested = pkg.requestedPermissions?.toList() ?: emptyList()
        val granted = mutableListOf<String>()
        pkg.requestedPermissionsFlags?.let { flags ->
            requested.forEachIndexed { i, perm ->
                if (i < flags.size && (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                    granted += perm
                }
            }
        }

        val minSdk = appInfo.minSdkVersion
        val targetSdk = appInfo.targetSdkVersion

        return ScannedApp(
            packageName = pkg.packageName,
            appName = label,
            versionName = pkg.versionName,
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkg.longVersionCode else @Suppress("DEPRECATION") pkg.versionCode.toLong(),
            apkPath = apkPath,
            apkSize = apkSize,
            sha256 = sha256,
            signerSha256 = signerSha256,
            installerLabel = installerLabel,
            isSideloaded = isSideloaded,
            isSystemApp = isSystem,
            isHidden = isHidden,
            hasLauncherIcon = hasLauncher,
            minSdk = minSdk,
            targetSdk = targetSdk,
            firstInstallTime = pkg.firstInstallTime,
            lastUpdateTime = pkg.lastUpdateTime,
            requestedPermissions = requested,
            grantedPermissions = granted,
            permissionsRequestedCount = requested.size,
            permissionsGrantedCount = granted.size
        )
    }
}
