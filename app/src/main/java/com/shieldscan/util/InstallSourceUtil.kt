package com.shieldscan.util

import android.content.pm.PackageManager
import android.os.Build

object InstallSourceUtil {
    fun getInstallerLabel(pm: PackageManager, pkg: String): Pair<String, Boolean> {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(pkg).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(pkg)
            }
            when (installer) {
                "com.android.vending" -> "Play Store" to false
                "com.amazon.venezia" -> "Amazon Appstore" to false
                "com.sec.android.app.samsungapps" -> "Galaxy Store" to false
                "org.fdroid.fdroid" -> "F-Droid" to false
                null -> "Sideloaded / Unknown" to true
                else -> installer to true
            }
        } catch (_: Exception) {
            "Unknown" to true
        }
    }
}
