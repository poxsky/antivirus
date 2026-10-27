package com.shieldscan.scanner

import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.util.regex.Pattern

data class ApkInspection(
    val packageName: String,
    val components: Components,
    val exportedCount: Int,
    val embeddedUrls: List<String>,
    val embeddedIps: List<String>
)

data class Components(
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>,
    val exportedActivities: List<String>,
    val exportedServices: List<String>,
    val exportedReceivers: List<String>
)

class ApkInspector(private val pm: PackageManager) {

    private val urlPattern = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
    private val ipPattern = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")

    fun inspect(packageName: String): ApkInspection? {
        return try {
            val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS

            val pkg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, flags)
            }

            val activities = pkg.activities?.map { it.name } ?: emptyList()
            val services = pkg.services?.map { it.name } ?: emptyList()
            val receivers = pkg.receivers?.map { it.name } ?: emptyList()
            val providers = pkg.providers?.map { it.name } ?: emptyList()

            val exportedActivities = pkg.activities?.filter { it.exported }?.map { it.name } ?: emptyList()
            val exportedServices = pkg.services?.filter { it.exported }?.map { it.name } ?: emptyList()
            val exportedReceivers = pkg.receivers?.filter { it.exported }?.map { it.name } ?: emptyList()

            val apkPath = pkg.applicationInfo?.sourceDir
            val strings = if (apkPath != null) extractStrings(File(apkPath)) else emptyList()

            val urls = strings.filter { urlPattern.matcher(it).find() }.take(50)
            val ips = strings.filter { ipPattern.matcher(it).find() }.take(50).filter { it.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) }

            ApkInspection(
                packageName = packageName,
                components = Components(activities, services, receivers, providers, exportedActivities, exportedServices, exportedReceivers),
                exportedCount = exportedActivities.size + exportedServices.size + exportedReceivers.size,
                embeddedUrls = urls,
                embeddedIps = ips
            )
        } catch (_: Exception) { null }
    }

    private fun extractStrings(apkFile: File): List<String> {
        return try {
            // Lightweight: read first 2MB of file as strings (DEX + manifest strings)
            val bytes = apkFile.inputStream().use { it.readBytes().take(2 * 1024 * 1024).toByteArray() }
            val text = String(bytes, Charsets.ISO_8859_1)
            text.split(Regex("[\\x00-\\x1F\\x7F]+")).filter { it.length in 6..120 }
        } catch (_: Exception) { emptyList() }
    }
}
