package com.nubiasms.messages.ui.update

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.nubiasms.messages.BuildConfig
import com.nubiasms.messages.update.GithubReleaseClient
import com.nubiasms.messages.update.UpdateChecker
import com.nubiasms.messages.update.UpdateDownloader
import com.nubiasms.messages.update.UpdateInfo
import com.nubiasms.messages.update.UpdateInstaller
import com.nubiasms.messages.update.UpdatePolicy
import com.nubiasms.messages.update.UpdatePromptKind
import com.nubiasms.messages.update.UpdateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UpdatePromptHost(manualCheckTick: Int = 0) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember {
        UpdateStore(context.getSharedPreferences("nubiasms_update", Context.MODE_PRIVATE))
    }
    val checker = remember {
        UpdateChecker(
            client =
                GithubReleaseClient(
                    owner = BuildConfig.UPDATE_GITHUB_OWNER,
                    repo = BuildConfig.UPDATE_GITHUB_REPO,
                    apkAssetName = BuildConfig.UPDATE_APK_ASSET,
                ),
            store = store,
            installedVersionCode = BuildConfig.VERSION_CODE,
        )
    }

    var promptKind by remember { mutableStateOf(UpdatePromptKind.None) }
    var info by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resumeEpoch by remember { mutableStateOf(0) }

    fun refresh(forceShowSoft: Boolean = false) {
        scope.launch {
            val kind = withContext(Dispatchers.IO) { checker.check() }
            val latest = store.lastInfo
            info = latest
            val raw =
                if (latest == null) {
                    UpdatePromptKind.None
                } else {
                    UpdatePolicy.displayKind(
                        policyKind = kind,
                        latestVersionCode = latest.versionCode,
                        softDismissedForVersionCode =
                            if (forceShowSoft) 0 else store.softDismissedForVersionCode,
                    )
                }
            promptKind = raw
            error = null
        }
    }

    LifecycleResumeEffect(Unit) {
        resumeEpoch += 1
        onPauseOrDispose { }
    }

    LaunchedEffect(resumeEpoch, manualCheckTick) {
        refresh(forceShowSoft = manualCheckTick > 0)
    }

    if (promptKind == UpdatePromptKind.None && error == null) return

    AlertDialog(
        onDismissRequest = {
            if (downloading) return@AlertDialog
            if (promptKind == UpdatePromptKind.Soft) {
                info?.let { store.softDismissedForVersionCode = it.versionCode }
            }
            promptKind = UpdatePromptKind.None
            error = null
        },
        title = { Text("Yeni güncelleme mevcut") },
        text = {
            when {
                downloading -> CircularProgressIndicator()
                error != null -> Text(error!!)
                else -> Text(info?.versionName?.let { "Sürüm $it" }.orEmpty())
            }
        },
        confirmButton = {
            TextButton(
                enabled = !downloading,
                onClick = {
                    val target = info ?: return@TextButton
                    downloading = true
                    error = null
                    scope.launch {
                        try {
                            val file =
                                withContext(Dispatchers.IO) {
                                    UpdateDownloader(context.cacheDir).download(target.apkDownloadUrl) { }
                                }
                            UpdateInstaller.install(context, file)
                            promptKind = UpdatePromptKind.None
                        } catch (e: Exception) {
                            error = e.message ?: "İndirme başarısız"
                        } finally {
                            downloading = false
                        }
                    }
                },
            ) {
                Text("Güncelle")
            }
        },
        dismissButton = {
            if (promptKind == UpdatePromptKind.Soft && !downloading) {
                TextButton(
                    onClick = {
                        info?.let { store.softDismissedForVersionCode = it.versionCode }
                        promptKind = UpdatePromptKind.None
                    },
                ) {
                    Text("Sonra")
                }
            }
        },
    )
}
