package com.nubiasms.messages.update

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GithubReleaseClientTest {
    private val releaseJson =
        """
        {
          "tag_name": "v0.1.3",
          "assets": [
            {
              "name": "update.json",
              "browser_download_url": "https://example.com/update.json"
            },
            {
              "name": "nubiasms-release.apk",
              "browser_download_url": "https://example.com/app.apk"
            }
          ]
        }
        """.trimIndent()

    @Test
    fun fetchLatest_returnsInfoFromAssets() =
        runBlocking {
            val bodies =
                mapOf(
                    "https://api.github.com/repos/acme/nubiaSMS/releases/latest" to releaseJson,
                    "https://example.com/update.json" to """{"versionCode":4,"versionName":"0.1.3"}""",
                )
            val client =
                GithubReleaseClient(
                    owner = "acme",
                    repo = "nubiaSMS",
                    apkAssetName = "nubiasms-release.apk",
                    openUrl = { url -> bodies[url] ?: error("unexpected $url") },
                )
            assertEquals(
                UpdateInfo(4, "0.1.3", "https://example.com/app.apk"),
                client.fetchLatest(),
            )
        }

    @Test
    fun fetchLatest_nullWhenApkMissing() =
        runBlocking {
            val noApk =
                """
                {"assets":[{"name":"update.json","browser_download_url":"https://example.com/update.json"}]}
                """.trimIndent()
            val client =
                GithubReleaseClient(
                    owner = "acme",
                    repo = "nubiaSMS",
                    apkAssetName = "nubiasms-release.apk",
                    openUrl = { noApk },
                )
            assertNull(client.fetchLatest())
        }
}
