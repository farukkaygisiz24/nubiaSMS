package com.nubiasms.messages.ui.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.nubiasms.messages.MainActivity
import com.nubiasms.messages.ui.components.myOsBounce
import com.nubiasms.messages.debug.TestSms
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nubiasms.messages.evidence.OtpEvidenceLog
import com.nubiasms.messages.sms.ConversationRepository
import com.nubiasms.messages.sms.ConversationState
import com.nubiasms.messages.sms.Folder
import com.nubiasms.messages.sms.SmsRoleHelper
import com.nubiasms.messages.ui.components.NubiaSmsTopBar
import com.nubiasms.messages.ui.theme.MyOsCardShape
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MyOS-style settings: grouped cards of 56dp rows with a grey subtitle and a chevron,
 * like the system Ayarlar pages.
 */
@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    onOpenFolder: (Folder) -> Unit = {},
    onOpenEvidence: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    val evidenceLog = remember { OtpEvidenceLog(File(context.filesDir, "evidence")) }
    val repo = remember { ConversationRepository(context.applicationContext) }

    var isDefault by remember { mutableStateOf(SmsRoleHelper.isDefaultSms(context)) }
    var autofillName by remember { mutableStateOf(autofillServiceName(context)) }
    var archivedCount by remember { mutableStateOf(ConversationState.archived(context).size) }
    var trashCount by remember { mutableStateOf(ConversationState.trash(context).size) }
    var evidenceCount by remember { mutableStateOf(evidenceLog.readAll().size) }
    var devEnabled by remember { mutableStateOf(DeveloperOptions.enabled(context)) }
    var versionTaps by remember { mutableStateOf(0) }
    var showTestSms by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isDefault = SmsRoleHelper.isDefaultSms(context)
                    autofillName = autofillServiceName(context)
                    archivedCount = ConversationState.archived(context).size
                    trashCount = ConversationState.trash(context).size
                    evidenceCount = evidenceLog.readAll().size
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        NubiaSmsTopBar(title = "Ayarlar", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .myOsBounce()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            GroupLabel("Mesajlar")
            Group {
                SettingRow(
                    title = "Arşiv",
                    subtitle = if (archivedCount == 0) "Boş" else "$archivedCount konuşma",
                    onClick = { onOpenFolder(Folder.Archive) },
                )
                Divider()
                SettingRow(
                    title = "Çöp kutusu",
                    subtitle =
                        (if (trashCount == 0) "Boş" else "$trashCount konuşma") +
                            " · ${ConversationState.RETENTION_DAYS} gün sonra kalıcı silinir",
                    onClick = { onOpenFolder(Folder.Trash) },
                )
                Divider()
                SettingRow(
                    title = "Spam",
                    subtitle = "Reklam mesajları · bildirim gelmez",
                    onClick = { onOpenFolder(Folder.Spam) },
                )
                Divider()
                SettingRow(
                    title = "Tümünü okundu işaretle",
                    chevron = false,
                    onClick = {
                        scope.launch {
                            val n = withContext(Dispatchers.IO) { repo.markAllRead() }
                            Toast.makeText(
                                context,
                                if (n == 0) "Okunmamış mesaj yok" else "$n konuşma okundu işaretlendi",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                )
            }

            GroupLabel("Sistem")
            Group {
                SettingRow(
                    title = "Varsayılan SMS uygulaması",
                    subtitle = if (isDefault) "nubiaSMS varsayılan" else "Dokunarak varsayılan yap",
                    onClick = { if (!isDefault) SmsRoleHelper.requestDefaultSms(activity) },
                    chevron = !isDefault,
                )
                Divider()
                SettingRow(
                    title = "Otomatik doldurma",
                    subtitle = autofillName?.let { "Şu an: $it" } ?: "Kapalı",
                    onClick = { openAutofillPicker(context) },
                )
                Divider()
                SettingRow(
                    title = "Güncellemeleri kontrol et",
                    onClick = { (context as? MainActivity)?.requestUpdateCheck() },
                )
            }

            GroupLabel("Gelişmiş")
            Group {
                SettingRow(
                    title = "OTP kanıt günlüğü",
                    subtitle = if (evidenceCount == 0) "Kayıt yok" else "$evidenceCount kayıt",
                    onClick = onOpenEvidence,
                )
            }

            if (devEnabled) {
                GroupLabel("Geliştirici")
                Group {
                    SettingRow(
                        title = "Test SMS gönder",
                        subtitle = "Gecikmeli sahte gelen mesaj; uygulama kapalıyken bildirim ve rozeti dener",
                        onClick = { showTestSms = true },
                    )
                    Divider()
                    SettingRow(
                        title = "Geliştirici seçeneklerini gizle",
                        chevron = false,
                        onClick = {
                            DeveloperOptions.setEnabled(context, false)
                            devEnabled = false
                            versionTaps = 0
                        },
                    )
                }
            }

            // Tapping the version 7 times unlocks the developer section (like Android).
            Text(
                text = "nubiaSMS ${appVersion(context)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (devEnabled) return@clickable
                            versionTaps++
                            val left = DeveloperOptions.TAPS_TO_UNLOCK - versionTaps
                            when {
                                left <= 0 -> {
                                    DeveloperOptions.setEnabled(context, true)
                                    devEnabled = true
                                    Toast.makeText(context, "Geliştirici seçenekleri açıldı", Toast.LENGTH_SHORT).show()
                                }
                                left <= 3 ->
                                    Toast.makeText(context, "Geliştirici olmanıza $left adım kaldı", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 24.dp),
            )
        }
    }

    if (showTestSms) {
        TestSmsDialog(
            onDismiss = { showTestSms = false },
            onSend = { address, body, delay ->
                showTestSms = false
                TestSms.schedule(context, address, body, delay)
                Toast.makeText(
                    context,
                    if (delay == 0) "Test SMS gönderildi" else "Test SMS $delay sn sonra gelecek. Uygulamayı kapatabilirsin.",
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
    }
}


@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MyOsCardShape)
            .background(MaterialTheme.colorScheme.surface),
    ) { content() }
}

@Composable
internal fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    chevron: Boolean = true,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (chevron) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Label of the system autofill service (e.g. "Proton Pass"); read-only, never changed here. */
private fun autofillServiceName(context: Context): String? {
    val flat = Settings.Secure.getString(context.contentResolver, "autofill_service") ?: return null
    val component = ComponentName.unflattenFromString(flat) ?: return null
    return try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(component.packageName, 0)).toString()
    } catch (_: Exception) {
        component.packageName
    }
}

private fun appVersion(context: Context): String =
    try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }

/**
 * ACTION_REQUEST_SET_AUTOFILL_SERVICE only resolves with a `package:` data Uri; without
 * it there is no matching activity and startActivity throws. Falls back to Settings.
 * This only opens the system picker; the user's choice (e.g. Proton Pass) is theirs.
 */
private fun openAutofillPicker(context: Context) {
    val picker =
        Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
            .setData(Uri.parse("package:${context.packageName}"))
    try {
        context.startActivity(picker)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            // Nothing to open; leave the user on this screen.
        }
    }
}
