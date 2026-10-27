package com.shieldscan.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtil {
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    fun formatDate(ts: Long): String {
        val fmt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        return fmt.format(Date(ts))
    }

    fun shortTime(ts: Long): String {
        val fmt = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
        return fmt.format(Date(ts))
    }
}
