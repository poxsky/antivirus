package com.shieldscan.ui.setup

import android.app.Application
import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SetupState(
    val hasUsageAccess: Boolean = false,
    val hasNotificationPerm: Boolean = false,
    val isBatteryUnrestricted: Boolean = false,
    val hasAccessibility: Boolean = false
)

class SetupViewModel(private val app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(SetupState())
    val state: StateFlow<SetupState> = _state

    fun refresh() {
        _state.value = SetupState(
            hasUsageAccess = hasUsageAccess(),
            hasNotificationPerm = NotificationManagerCompat.from(app).areNotificationsEnabled(),
            isBatteryUnrestricted = isBatteryUnrestricted(),
            hasAccessibility = hasAccessibility()
        )
    }

    private fun hasUsageAccess(): Boolean {
        return try {
            val appOps = app.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), app.packageName)
            } else {
                @Suppress("DEPRECATION") appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), app.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) { false }
    }

    private fun isBatteryUnrestricted(): Boolean {
        return try {
            val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(app.packageName)
        } catch (_: Exception) { false }
    }

    private fun hasAccessibility(): Boolean {
        val enabled = Settings.Secure.getString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.isNotBlank()
    }
}
