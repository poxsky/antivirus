package com.shieldscan.risk

import android.content.pm.ApplicationInfo

object CategoryExpectations {

    enum class AppCategory {
        GAME, SOCIAL, COMMUNICATION, MAPS, MUSIC_AUDIO, VIDEO, PHOTOGRAPHY,
        PRODUCTIVITY, FINANCE, SHOPPING, HEALTH, EDUCATION, NEWS, TRAVEL,
        TOOLS_UTILITY, PERSONALIZATION, UNKNOWN
    }

    fun fromApplicationInfo(ai: ApplicationInfo): AppCategory {
        return when (ai.category) {
            ApplicationInfo.CATEGORY_GAME -> AppCategory.GAME
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_MAPS -> AppCategory.MAPS
            ApplicationInfo.CATEGORY_AUDIO -> AppCategory.MUSIC_AUDIO
            ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
            ApplicationInfo.CATEGORY_IMAGE -> AppCategory.PHOTOGRAPHY
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
            ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
            else -> AppCategory.UNKNOWN
        }
    }

    fun inferFromPackage(pkg: String, appName: String): AppCategory {
        val lower = "$pkg $appName".lowercase()
        return when {
            listOf("game", "ludo", "poker", "clash", "pubg", "freefire").any { it in lower } -> AppCategory.GAME
            listOf("map", "navigation", "ola", "uber", "rapido").any { it in lower } -> AppCategory.MAPS
            listOf("camera", "photo", "gallery", "editor").any { it in lower } -> AppCategory.PHOTOGRAPHY
            listOf("music", "audio", "spotify", "gaana", "wynk").any { it in lower } -> AppCategory.MUSIC_AUDIO
            listOf("video", "player", "youtube", "mxplayer", "vlc").any { it in lower } -> AppCategory.VIDEO
            listOf("torch", "flashlight", "calculator", "cleaner", "booster", "wallpaper", "keyboard").any { it in lower } -> AppCategory.TOOLS_UTILITY
            listOf("bank", "pay", "upi", "phonepe", "gpay", "paytm").any { it in lower } -> AppCategory.FINANCE
            listOf("whatsapp", "telegram", "messenger", "instagram", "facebook", "twitter").any { it in lower } -> AppCategory.SOCIAL
            else -> AppCategory.UNKNOWN
        }
    }

    // Minimal expected permissions per category - anything beyond is suspicious
    val expectedPermissions: Map<AppCategory, Set<String>> = mapOf(
        AppCategory.GAME to setOf(
            "android.permission.INTERNET",
            "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK",
            "android.permission.ACCESS_NETWORK_STATE"
        ),
        AppCategory.SOCIAL to setOf(
            "android.permission.INTERNET", "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO", "android.permission.READ_CONTACTS",
            "android.permission.ACCESS_FINE_LOCATION", "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.ACCESS_NETWORK_STATE"
        ),
        AppCategory.MAPS to setOf(
            "android.permission.INTERNET", "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_NETWORK_STATE"
        ),
        AppCategory.MUSIC_AUDIO to setOf(
            "android.permission.INTERNET", "android.permission.RECORD_AUDIO",
            "android.permission.READ_EXTERNAL_STORAGE", "android.permission.FOREGROUND_SERVICE",
            "android.permission.ACCESS_NETWORK_STATE"
        ),
        AppCategory.VIDEO to setOf(
            "android.permission.INTERNET", "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO", "android.permission.READ_EXTERNAL_STORAGE"
        ),
        AppCategory.PHOTOGRAPHY to setOf(
            "android.permission.CAMERA", "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.INTERNET"
        ),
        AppCategory.PRODUCTIVITY to setOf(
            "android.permission.INTERNET", "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.CAMERA"
        ),
        AppCategory.FINANCE to setOf(
            "android.permission.INTERNET", "android.permission.CAMERA",
            "android.permission.USE_BIOMETRIC", "android.permission.NFC",
            "android.permission.READ_CONTACTS", "android.permission.ACCESS_NETWORK_STATE"
        ),
        AppCategory.TOOLS_UTILITY to setOf(
            "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.VIBRATE"
        ),
        AppCategory.UNKNOWN to setOf(
            "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE"
        )
    )

    fun getExpected(category: AppCategory): Set<String> =
        expectedPermissions[category] ?: expectedPermissions[AppCategory.UNKNOWN]!!

    // Dangerous permissions that should never be in TOOLS_UTILITY
    val utilitySensitive = setOf(
        "android.permission.READ_SMS", "android.permission.SEND_SMS",
        "android.permission.READ_CONTACTS", "android.permission.READ_CALL_LOG",
        "android.permission.ACCESS_FINE_LOCATION", "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA", "android.permission.READ_PHONE_STATE"
    )
}
