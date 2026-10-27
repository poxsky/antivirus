package com.shieldscan.risk

import com.shieldscan.data.RiskLevel
import com.shieldscan.data.RiskResult
import com.shieldscan.data.ScannedApp
import com.shieldscan.scanner.SpecialAccessState

object RiskEngine {

    // Whitelist of known safe popular apps - never flag as critical
    private val trustedPackages = setOf(
        "com.whatsapp", "com.facebook.katana", "com.instagram.android",
        "com.google.android.gm", "com.google.android.youtube",
        "com.google.android.apps.maps", "com.spotify.music",
        "com.netflix.mediaclient", "com.twitter.android",
        "com.snapchat.android", "com.paypal.android.p2pmobile",
        "com.phonepe.app", "net.one97.paytm", "com.google.android.apps.nbu.paisa.user",
        "com.uber.android", "com.olacabs.customer", "com.zomato",
        "com.swiggy.android", "com.amazon.mShop.android.shopping",
        "com.flipkart.android", "com.google.android.chrome",
        "org.mozilla.firefox", "com.microsoft.office.outlook"
    )

    fun assess(
        app: ScannedApp,
        special: SpecialAccessState,
        category: CategoryExpectations.AppCategory
    ): RiskResult {
        val granted = app.grantedPermissions.toSet()
        val reasons = mutableListOf<String>()
        var score = 0

        // 1) Special access weights
        if (special.hasAccessibility) {
            score += 40; reasons += "♿ Accessibility service enabled — can read screen & auto-click"
        }
        if (special.isDeviceAdmin) {
            score += 35; reasons += "👑 Device Admin active — can lock device & prevent uninstall"
        }
        if (special.hasNotificationListener) {
            score += 25; reasons += "🔔 Reads notifications — can steal OTPs"
        }
        if (special.hasOverlay) {
            score += 25; reasons += "🪟 Draws over other apps — can show fake login screens"
        }
        if (special.canInstallUnknownApps) {
            score += 25; reasons += "📥 Can install unknown apps — can drop more malware"
        }

        // 2) Permission weights
        fun addIf(perm: String, w: Int, label: String) {
            if (perm in granted) { score += w; reasons += label }
        }
        addIf("android.permission.READ_SMS", 30, "💬 Can read SMS — OTP interception risk")
        addIf("android.permission.SEND_SMS", 30, "💬 Can send SMS — premium fraud risk")
        addIf("android.permission.RECEIVE_SMS", 28, "💬 Can receive SMS silently")
        addIf("android.permission.READ_CONTACTS", 15, "👥 Access to contacts")
        addIf("android.permission.READ_CALL_LOG", 15, "📞 Access to call history")
        addIf("android.permission.READ_PHONE_STATE", 12, "📱 Can read phone state / IMEI")
        addIf("android.permission.CALL_PHONE", 18, "📞 Can make phone calls")
        addIf("android.permission.PROCESS_OUTGOING_CALLS", 18, "📞 Can intercept outgoing calls")

        // Mismatched sensitive perms for category
        val expected = CategoryExpectations.getExpected(category)
        val unexpected = granted.filter { it !in expected && it.startsWith("android.permission.") }
        if (unexpected.isNotEmpty() && category == CategoryExpectations.AppCategory.TOOLS_UTILITY) {
            score += 20; reasons += "⚠️ Torch/utility app requesting: ${unexpected.take(3).joinToString()} — not needed"
        } else if (unexpected.size >= 3) {
            score += 12; reasons += "⚠️ Requests ${unexpected.size} permissions beyond category need"
        }

        // Location/mic/camera if not expected
        if ("android.permission.ACCESS_FINE_LOCATION" in granted && category !in setOf(CategoryExpectations.AppCategory.MAPS, CategoryExpectations.AppCategory.SOCIAL)) {
            score += 20; reasons += "📍 Location access — not typical for ${category.name}"
        }
        if ("android.permission.RECORD_AUDIO" in granted && category !in setOf(CategoryExpectations.AppCategory.MUSIC_AUDIO, CategoryExpectations.AppCategory.VIDEO, CategoryExpectations.AppCategory.SOCIAL)) {
            score += 20; reasons += "🎙️ Microphone access — unusual for ${category.name}"
        }
        if ("android.permission.CAMERA" in granted && category !in setOf(CategoryExpectations.AppCategory.PHOTOGRAPHY, CategoryExpectations.AppCategory.SOCIAL, CategoryExpectations.AppCategory.VIDEO)) {
            score += 15; reasons += "📷 Camera access — unusual for ${category.name}"
        }

        // 3) Provenance signals
        if (app.isSideloaded) { score += 20; reasons += "📦 Sideloaded — not from Play Store (${app.installerLabel})" }
        if (app.isHidden) { score += 30; reasons += "🫥 No launcher icon — hidden from app drawer" }
        if (app.targetSdk <= 22) { score += 10; reasons += "🕰️ Targets Android ${app.targetSdk} — bypasses runtime permissions" }
        if (app.appName.lowercase() in setOf("system update", "wifi service", "google service", "android update") && !app.isSystemApp) {
            score += 25; reasons += "🎭 Fake system name \"${app.appName}\" but not a system app"
        }

        // 4) Combo rules
        val combos = ComboRules.evaluate(granted, app.isHidden, app.isSideloaded, app.targetSdk, category)
        combos.forEach { rule ->
            score += (rule.weight / 3) // add fractional to avoid double-count dominance
            reasons += "🚨 ${rule.verdict}"
        }

        // Cap 0..100
        score = score.coerceIn(0, 100)

        // Trusted whitelist dampening - cap at 60 unless truly critical combo
        if (app.packageName in trustedPackages) {
            if (combos.none { it.id in setOf("banking_trojan", "rat_stalkerware") }) {
                score = minOf(score, 60)
                if (reasons.size > 3) reasons += "ℹ️ Popular app — score dampened (whitelisted)"
            }
        }

        val level = when (score) {
            in 0..30 -> RiskLevel.SAFE
            in 31..60 -> RiskLevel.REVIEW
            in 61..85 -> RiskLevel.RISKY
            else -> RiskLevel.CRITICAL
        }

        // Deduplicate reasons
        val dedup = reasons.distinct()

        return RiskResult(score, level, dedup, combos.map { it.description })
    }
}
