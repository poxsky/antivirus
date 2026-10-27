package com.shieldscan.util

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object HashUtil {
    fun sha256(file: File): String {
        if (!file.exists()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buf = ByteArray(8192)
            var n: Int
            while (fis.read(buf).also { n = it } != -1) {
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().toHex()
    }

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
