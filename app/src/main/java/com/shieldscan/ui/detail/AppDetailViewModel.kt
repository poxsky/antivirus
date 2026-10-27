package com.shieldscan.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shieldscan.ShieldScanApp
import com.shieldscan.data.db.AppEntity
import com.shieldscan.risk.CategoryExpectations
import com.shieldscan.scanner.ApkInspector

data class AppDetailState(
    val entity: AppEntity,
    val requested: List<String>,
    val granted: List<String>,
    val expected: Set<String>,
    val reasons: List<String>,
    val minSdk: Int,
    val targetSdk: Int,
    val embeddedUrls: List<String>,
    val embeddedIps: List<String>
)

class AppDetailViewModel(private val app: Application) : AndroidViewModel(app) {
    private val db = (app as ShieldScanApp).db
    private val gson = Gson()

    suspend fun load(pkg: String): AppDetailState? {
        val e = db.appDao().getByPackage(pkg) ?: return null
        val granted: List<String> = try { gson.fromJson(e.grantedPermissionsJson, object:TypeToken<List<String>>(){}.type) } catch (_:Exception) { emptyList() }
        val reasons: List<String> = try { gson.fromJson(e.reasonsJson, object:TypeToken<List<String>>(){}.type) } catch (_:Exception) { emptyList() }

        // requested - approximate from granted + heuristic; full requested not stored separately to save space, reconstruct from pm if needed
        // For now granted == requested subset, expand via live PM if available
        val requested = try {
            val pm = app.packageManager
            val pi = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, android.content.pm.PackageManager.PackageInfoFlags.of(android.content.pm.PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION") pm.getPackageInfo(pkg, android.content.pm.PackageManager.GET_PERMISSIONS)
            }
            pi.requestedPermissions?.toList() ?: granted
        } catch (_: Exception) { granted }

        val category = try {
            val ai = app.packageManager.getApplicationInfo(pkg, 0)
            val c1 = CategoryExpectations.fromApplicationInfo(ai)
            if (c1 == CategoryExpectations.AppCategory.UNKNOWN) CategoryExpectations.inferFromPackage(pkg, e.appName) else c1
        } catch (_: Exception) { CategoryExpectations.AppCategory.UNKNOWN }

        val expected = CategoryExpectations.getExpected(category)

        // Apk inspection
        val inspector = ApkInspector(app.packageManager)
        val insp = inspector.inspect(pkg)

        return AppDetailState(
            entity = e,
            requested = requested,
            granted = granted,
            expected = expected,
            reasons = reasons,
            minSdk = e.apkPath.let { // fallback: try to read from app info
                try { app.packageManager.getApplicationInfo(pkg, 0).minSdkVersion } catch (_:Exception) { 21 }
            },
            targetSdk = try { app.packageManager.getApplicationInfo(pkg, 0).targetSdkVersion } catch (_:Exception) { 30 },
            embeddedUrls = insp?.embeddedUrls ?: emptyList(),
            embeddedIps = insp?.embeddedIps ?: emptyList()
        )
    }
}
