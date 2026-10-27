package com.shieldscan.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.shieldscan.workers.ScanScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, "android.intent.action.PACKAGE_REPLACED")) {
            // Reschedule workers
            ScanScheduler.ensureScheduled(context)
            // Restart foreground watch
            try {
                val svc = Intent(context, InstallWatchService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(svc)
                else context.startService(svc)
            } catch (_: Exception) {}
        }
    }
}
