package com.shieldscan.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ScanScheduler {
    const val WORK_MORNING = "morning_scan"
    const val WORK_DIFF = "diff_scan"

    fun ensureScheduled(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)

        // Morning: every 24h, battery not low
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 7); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        val initialDelay = target.timeInMillis - now.timeInMillis

        val morning = PeriodicWorkRequestBuilder<MorningScanWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .addTag("shieldscan")
            .build()

        wm.enqueueUniquePeriodicWork(WORK_MORNING, ExistingPeriodicWorkPolicy.KEEP, morning)

        // Diff safety net: every 15 min
        val diff = PeriodicWorkRequestBuilder<DiffWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
            .addTag("shieldscan")
            .build()
        wm.enqueueUniquePeriodicWork(WORK_DIFF, ExistingPeriodicWorkPolicy.KEEP, diff)
    }

    fun runNow(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        val oneOff = androidx.work.OneTimeWorkRequestBuilder<MorningScanWorker>().build()
        wm.enqueue(oneOff)
    }
}
