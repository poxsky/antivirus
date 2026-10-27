package com.shieldscan.scanner

import android.app.AppOpsManager
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager

data class BehaviorSnapshot(
    val txBytesWifi: Long = 0,
    val rxBytesWifi: Long = 0,
    val txBytesMobile: Long = 0,
    val rxBytesMobile: Long = 0,
    val backgroundRunning: Boolean = false,
    val lastTimeVisible: Long = 0
)

data class AnomalyResult(
    val isAnomalous: Boolean,
    val reasons: List<String>
)

class BehaviorMonitor(private val ctx: Context) {

    fun getNetworkStats(packageName: String, uid: Int, windowMs: Long = 7 * 24 * 3600 * 1000L): BehaviorSnapshot {
        return try {
            val nsm = ctx.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
            val now = System.currentTimeMillis()
            val start = now - windowMs
            var txW = 0L; var rxW = 0L; var txM = 0L; var rxM = 0L

            // WiFi
            try {
                val bucket = nsm.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, start, now)
                // Fallback per uid query
                val summary = nsm.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, null, start, now, uid)
                while (summary.hasNextBucket()) {
                    val b = android.app.usage.NetworkStats.Bucket()
                    summary.getNextBucket(b)
                    txW += b.txBytes
                    rxW += b.rxBytes
                }
                summary.close()
            } catch (_: Exception) { }

            // Mobile
            try {
                val summary = nsm.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE, null, start, now, uid)
                while (summary.hasNextBucket()) {
                    val b = android.app.usage.NetworkStats.Bucket()
                    summary.getNextBucket(b)
                    txM += b.txBytes
                    rxM += b.rxBytes
                }
                summary.close()
            } catch (_: Exception) { }

            BehaviorSnapshot(txW, rxW, txM, rxM)
        } catch (_: Exception) {
            BehaviorSnapshot()
        }
    }

    fun getUsageStats(packageName: String): BehaviorSnapshot {
        return try {
            val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val start = now - 7 * 24 * 3600 * 1000L
            val events = usm.queryEvents(start, now)
            var lastVisible = 0L
            var background = false
            val ev = android.app.usage.UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(ev)
                if (ev.packageName == packageName) {
                    when (ev.eventType) {
                        android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND -> lastVisible = ev.timeStamp
                        android.app.usage.UsageEvents.Event.FOREGROUND_SERVICE_START -> background = true
                    }
                }
            }
            BehaviorSnapshot(backgroundRunning = background, lastTimeVisible = lastVisible)
        } catch (_: Exception) {
            BehaviorSnapshot()
        }
    }

    fun detectAnomalies(
        packageName: String,
        behavior: BehaviorSnapshot,
        net: BehaviorSnapshot,
        lastUpdateTime: Long
    ): AnomalyResult {
        val reasons = mutableListOf<String>()
        val now = System.currentTimeMillis()
        val totalTx = net.txBytesWifi + net.txBytesMobile

        // Night upload: if large tx in last window and app updated recently at night? heuristic: tx > 50MB
        if (totalTx > 50 * 1024 * 1024L) {
            reasons += "High background data: ${totalTx / (1024*1024)} MB in 7 days"
        }

        // Dormant but network active
        if (behavior.lastTimeVisible == 0L && totalTx > 5 * 1024 * 1024L) {
            reasons += "Never opened by you but uses network — ghost app?"
        }

        // Not visible for 7 days but background running
        if (behavior.lastTimeVisible != 0L && (now - behavior.lastTimeVisible) > 7 * 24 * 3600 * 1000L && behavior.backgroundRunning) {
            reasons += "Not opened in 7+ days but runs in background"
        }

        return AnomalyResult(reasons.isNotEmpty(), reasons)
    }
}
