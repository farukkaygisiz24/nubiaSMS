package com.aramalr.messages.ui.compose

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aramalr.messages.sms.ConversationRepository
import com.aramalr.messages.ui.components.AramalrTopBar
import com.aramalr.messages.ui.components.MyOsCard

@Composable
fun NewMessageScreen(
    onBack: () -> Unit,
    onOpenedThread: (Long) -> Unit,
    onSend: (address: String, body: String, threadId: Long) -> Unit = { _, _, threadId ->
        onOpenedThread(threadId)
    },
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val repo = remember { ConversationRepository(app) }
    var address by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    val canSend = address.trim().isNotEmpty() && body.trim().isNotEmpty()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp),
    ) {
        AramalrTopBar(title = "Yeni mesaj", onBack = onBack)
        Spacer(Modifier.height(12.dp))
        MyOsCard {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "Alıcı",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = address,
                    onValueChange = { address = it },
                    singleLine = true,
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (address.isEmpty()) {
                            Text(
                                text = "Ad veya telefon numarası",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        MyOsCard(modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(vertical = 12.dp).fillMaxSize()) {
                Text(
                    text = "Mesaj",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = body,
                    onValueChange = { body = it },
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { inner ->
                        if (body.isEmpty()) {
                            Text(
                                text = "Mesaj yazın",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            FloatingActionButton(
                onClick = {
                    if (!canSend) return@FloatingActionButton
                    val to = address.trim()
                    val text = body.trim()
                    val threadId = repo.getOrCreateThreadId(to)
                    onSend(to, text, threadId)
                },
                shape = CircleShape,
                containerColor =
                    if (canSend) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                contentColor =
                    if (canSend) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(64.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder")
                    Text(text = "SMS", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
