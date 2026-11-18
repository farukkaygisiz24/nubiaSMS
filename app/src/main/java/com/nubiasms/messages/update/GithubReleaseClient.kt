package com.nubiasms.messages.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GithubReleaseClient(
    private val owner: String,
    private val repo: String,
    private val apkAssetName: String,
    private val openUrl: (String) -> String = ::httpGet,
) {
    suspend fun fetchLatest(): UpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val releaseBody = openUrl("https://api.github.com/repos/$owner/$repo/releases/latest")
                val root = JSONObject(releaseBody)
                val assets = root.optJSONArray("assets") ?: return@withContext null
                var updateJsonUrl: String? = null
                var apkUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    when (asset.optString("name")) {
                        "update.json" -> updateJsonUrl = asset.optString("browser_download_url")
                        apkAssetName -> apkUrl = asset.optString("browser_download_url")
                    }
                }
                if (updateJsonUrl.isNullOrBlank() || apkUrl.isNullOrBlank()) return@withContext null
                val parsed = UpdateJsonParser.parse(openUrl(updateJsonUrl)) ?: return@withContext null
                UpdateInfo(parsed.first, parsed.second, apkUrl)
            } catch (_: Exception) {
                null
            }
        }

    companion object {
        fun httpGet(url: String): String {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "nubiaSMS-updater")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            try {
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                return BufferedReader(InputStreamReader(stream)).use { it.readText() }.also {
                    if (code !in 200..299) error("HTTP $code")
                }
            } finally {
                conn.disconnect()
            }
        }
    }
}
