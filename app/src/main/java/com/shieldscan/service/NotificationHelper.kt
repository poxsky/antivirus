package com.shieldscan.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.shieldscan.MainActivity
import com.shieldscan.R

object NotificationHelper {
    const val CH_SCAN = "shield_scan"
    const val CH_ALERT = "shield_alert"
    const val CH_SERVICE = "shield_service"
    const val ID_MORNING = 1001
    const val ID_NEW_APP = 1002
    const val ID_SERVICE = 1003

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CH_SCAN, "Scans", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Daily scan summaries"
        })
        nm.createNotificationChannel(NotificationChannel(CH_ALERT, "Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "New risky apps and anomalies"
        })
        nm.createNotificationChannel(NotificationChannel(CH_SERVICE, "Shield Service", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Install watch foreground service"
            setShowBadge(false)
        })
    }

    fun showMorningSummary(ctx: Context, total: Int, risky: Int, critical: Int, sideloaded: Int) {
        ensureChannels(ctx)
        val intent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = "$total apps scanned • $critical critical, $risky risky • $sideloaded sideloaded"
        val notif = NotificationCompat.Builder(ctx, CH_SCAN)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("🌅 Morning Scan Complete — ${if (critical > 0) "Action Needed" else "All Clear"}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(ID_MORNING, notif)
    }

    fun showNewAppAlert(ctx: Context, appName: String, pkg: String, score: Int, isSideloaded: Boolean) {
        ensureChannels(ctx)
        val intent = Intent(ctx, MainActivity::class.java).apply { putExtra("highlight_pkg", pkg) }
        val pi = PendingIntent.getActivity(ctx, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val deleteIntent = Intent(Intent.ACTION_DELETE).apply { data = android.net.Uri.parse("package:$pkg") }
        val deletePi = PendingIntent.getActivity(ctx, 2, deleteIntent, PendingIntent.FLAG_IMMUTABLE)

        val src = if (isSideloaded) "Unknown (sideloaded)" else "Store"
        val notif = NotificationCompat.Builder(ctx, CH_ALERT)
            .setSmallIcon(R.drawable.ic_shield_alert)
            .setContentTitle("🚨 New app: \"$appName\"")
            .setContentText("Source: $src · Risk: $score/100 — tap to review")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Package: $pkg\nSource: $src\nRisk: $score/100\nTap Details to inspect permissions & behavior."))
            .setContentIntent(pi)
            .addAction(R.drawable.ic_shield, "Details", pi)
            .addAction(R.drawable.ic_delete, "Uninstall", deletePi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(ID_NEW_APP + pkg.hashCode(), notif)
    }

    fun foregroundNotification(ctx: Context): android.app.Notification {
        ensureChannels(ctx)
        val intent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(ctx, CH_SERVICE)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("ShieldScan watching installs")
            .setContentText("Monitoring for new apps in real-time")
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }
}
