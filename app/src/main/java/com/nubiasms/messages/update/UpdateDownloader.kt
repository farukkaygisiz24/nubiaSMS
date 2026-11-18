package com.nubiasms.messages.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UpdateDownloader(private val cacheDir: File) {
    suspend fun download(url: String, onProgress: (Float?) -> Unit): File =
        withContext(Dispatchers.IO) {
            val out = File(cacheDir, "update-download.apk")
            if (out.exists()) out.delete()
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "nubiaSMS-updater")
                connectTimeout = 30_000
                readTimeout = 60_000
            }
            try {
                if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
                val total = conn.contentLengthLong.takeIf { it > 0 }
                conn.inputStream.use { input ->
                    out.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var read = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            read += n
                            onProgress(total?.let { read.toFloat() / it })
                        }
                        output.flush()
                    }
                }
                onProgress(1f)
                out
            } finally {
                conn.disconnect()
            }
        }
}
