package com.shieldscan.net

import com.shieldscan.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class VtResult(
    val positives: Int,
    val total: Int,
    val permalink: String?,
    val isMalicious: Boolean
)

class VirusTotalClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun lookupHash(sha256: String, apiKey: String = BuildConfig.VT_API_KEY): VtResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || sha256.isBlank()) return@withContext null
        try {
            val req = Request.Builder()
                .url("https://www.virustotal.com/api/v3/files/$sha256")
                .header("x-apikey", apiKey)
                .get()
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                // 404 means not found = not malicious per VT
                if (resp.code == 404) return@withContext VtResult(0, 0, null, false)
                return@withContext null
            }
            val body = resp.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val data = json.optJSONObject("data") ?: return@withContext null
            val attrs = data.optJSONObject("attributes") ?: return@withContext null
            val stats = attrs.optJSONObject("last_analysis_stats")
            val malicious = stats?.optInt("malicious", 0) ?: 0
            val total = listOf("harmless", "malicious", "suspicious", "undetected", "timeout")
                .sumOf { stats?.optInt(it, 0) ?: 0 }
            val permalink = "https://www.virustotal.com/gui/file/$sha256"
            VtResult(malicious, total, permalink, malicious > 0)
        } catch (_: Exception) { null }
    }
}
