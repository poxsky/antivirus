package com.shieldscan.risk

data class ComboRule(
    val id: String,
    val description: String,
    val verdict: String,
    val requiredPermissions: Set<String>,
    val requireHidden: Boolean = false,
    val requireSideloaded: Boolean = false,
    val requireOldTarget: Boolean = false,
    val weight: Int
)

object ComboRules {
    val all = listOf(
        ComboRule(
            id = "banking_trojan",
            description = "SMS + Overlay pattern (banking trojan)",
            verdict = "Banking trojan pattern — can steal OTPs via fake screens",
            requiredPermissions = setOf(
                "android.permission.READ_SMS",
                "android.permission.SYSTEM_ALERT_WINDOW"
            ),
            weight = 85
        ),
        ComboRule(
            id = "rat_stalkerware",
            description = "Accessibility + hidden icon",
            verdict = "RAT / stalkerware pattern — screen reading while hidden",
            requiredPermissions = setOf(
                "android.permission.BIND_ACCESSIBILITY_SERVICE"
            ),
            requireHidden = true,
            weight = 90
        ),
        ComboRule(
            id = "sms_stealer",
            description = "SMS read/send + sideloaded",
            verdict = "SMS stealer — reads OTPs and sends premium SMS",
            requiredPermissions = setOf(
                "android.permission.READ_SMS",
                "android.permission.SEND_SMS"
            ),
            requireSideloaded = true,
            weight = 80
        ),
        ComboRule(
            id = "spyware_location_mic",
            description = "Location + Microphone + Camera + contacts",
            verdict = "Spyware pattern — tracks location, audio, camera",
            requiredPermissions = setOf(
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.RECORD_AUDIO",
                "android.permission.CAMERA"
            ),
            weight = 75
        ),
        ComboRule(
            id = "device_admin_abuse",
            description = "Device admin + overlay + sideloaded",
            verdict = "Device admin abuse — prevents uninstall, draws over apps",
            requiredPermissions = setOf(
                "android.permission.SYSTEM_ALERT_WINDOW"
            ),
            requireSideloaded = true,
            weight = 70
        ),
        ComboRule(
            id = "utility_overreach",
            description = "Utility app with dangerous permissions",
            verdict = "Utility app requesting dangerous permissions it doesn't need",
            requiredPermissions = setOf(
                "android.permission.READ_SMS"
            ),
            weight = 60
        ),
        ComboRule(
            id = "call_log_harvester",
            description = "Call log + contacts + phone state",
            verdict = "Harvester — collects call logs and contacts",
            requiredPermissions = setOf(
                "android.permission.READ_CALL_LOG",
                "android.permission.READ_CONTACTS",
                "android.permission.READ_PHONE_STATE"
            ),
            weight = 65
        ),
        ComboRule(
            id = "old_sdk_sideload",
            description = "Old target SDK + sideloaded + broad permissions",
            verdict = "Legacy SDK + sideloaded — bypasses runtime permissions",
            requiredPermissions = emptySet(),
            requireSideloaded = true,
            requireOldTarget = true,
            weight = 50
        )
    )

    fun evaluate(
        grantedPerms: Set<String>,
        isHidden: Boolean,
        isSideloaded: Boolean,
        targetSdk: Int,
        category: CategoryExpectations.AppCategory
    ): List<ComboRule> {
        return all.filter { rule ->
            val permsMatch = rule.requiredPermissions.isEmpty() ||
                rule.requiredPermissions.all { it in grantedPerms } ||
                (rule.requiredPermissions.size == 1 && rule.requiredPermissions.any { it in grantedPerms })

            // Special handling for rules that need multiple signals
            val hiddenOk = !rule.requireHidden || isHidden
            val sideloadOk = !rule.requireSideloaded || isSideloaded
            val oldSdkOk = !rule.requireOldTarget || targetSdk <= 23

            // Banking trojan also needs at least one of SMS variants
            val specialOk = when (rule.id) {
                "banking_trojan" -> grantedPerms.any { it.contains("SMS") || it.contains("RECEIVE_SMS") }
                "utility_overreach" -> category == CategoryExpectations.AppCategory.TOOLS_UTILITY &&
                    grantedPerms.any { it in CategoryExpectations.utilitySensitive }
                else -> true
            }

            permsMatch && hiddenOk && sideloadOk && oldSdkOk && specialOk
        }
    }
}
