package com.aramalr.messages.ui.settings

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aramalr.messages.evidence.OtpEvidenceEvent
import com.aramalr.messages.evidence.OtpEvidenceLog
import com.aramalr.messages.sms.SmsRoleHelper
import com.aramalr.messages.ui.components.AramalrTopBar
import com.aramalr.messages.ui.components.MyOsCard
import java.io.File

@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val evidenceLog = remember { OtpEvidenceLog(File(context.filesDir, "evidence")) }
    var isDefault by remember { mutableStateOf(SmsRoleHelper.isDefaultSms(context)) }
    var events by remember { mutableStateOf(evidenceLog.readAll().asReversed()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isDefault = SmsRoleHelper.isDefaultSms(context)
                    events = evidenceLog.readAll().asReversed()
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        AramalrTopBar(title = "Ayarlar", onBack = onBack)
        Spacer(Modifier.height(16.dp))
        MyOsCard {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "Varsayılan SMS uygulaması",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { SmsRoleHelper.requestDefaultSms(activity) }
                            .padding(vertical = 8.dp),
                )
                Text(
                    text =
                        if (isDefault) {
                            "aramalr varsayılan"
                        } else {
                            "Dokunarak varsayılan yap (Google Messages silinmez)"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        MyOsCard {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "Otomatik doldurma (Autofill)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE))
                            }
                            .padding(vertical = 8.dp),
                )
                Text(
                    text = "Sistem ayarlarından aramalr'ı Autofill servisi yap",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        MyOsCard {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "OTP kanıt günlüğü",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Temizle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .clickable {
                                evidenceLog.clear()
                                events = emptyList()
                            }
                            .padding(vertical = 8.dp),
                )
                if (events.isEmpty()) {
                    Text(
                        text = "Henüz kayıt yok",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    events.take(40).forEach { event ->
                        EvidenceRow(event)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EvidenceRow(event: OtpEvidenceEvent) {
    Column {
        Text(
            text = "${event.sender} · ${event.assistPath}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text =
                if (event.detectedCode != null) {
                    "kod: ${event.detectedCode}"
                } else {
                    "kod yok"
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
