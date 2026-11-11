package com.aramalr.messages.ui.thread

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aramalr.messages.R
import com.aramalr.messages.sms.Message
import com.aramalr.messages.sms.SmsAddress
import com.aramalr.messages.ui.components.AramalrTopBar
import com.aramalr.messages.ui.components.MyOsCard

@Composable
fun ThreadScreen(
    viewModel: ThreadViewModel,
    onBack: () -> Unit,
    onSend: (String) -> Unit = {},
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val address = messages.firstOrNull()?.address.orEmpty()
    val title = address.ifBlank { "Sohbet" }
    val acceptsReplies = SmsAddress.acceptsReplies(address)
    val canSend = draft.trim().isNotEmpty()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
    ) {
        AramalrTopBar(
            title = title,
            onBack = onBack,
        )
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    onCopyOtp = { code ->
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("otp", code))
                    },
                )
            }
        }
        if (!acceptsReplies && address.isNotBlank()) {
            MyOsCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = context.getString(R.string.sender_no_reply),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
            }
        } else {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) {
                            Text(
                                text = "Mesaj",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    },
                )
                FloatingActionButton(
                    onClick = {
                        val body = draft.trim()
                        if (body.isEmpty()) return@FloatingActionButton
                        onSend(body)
                        draft = ""
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
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    onCopyOtp: (String) -> Unit,
) {
    val align = if (message.isOutgoing) Alignment.End else Alignment.Start
    val bg =
        if (message.isOutgoing) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Text(
            text = message.body,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
        )
        val otp = message.otp
        if (otp != null) {
            Spacer(Modifier.height(4.dp))
            AssistChip(
                onClick = { onCopyOtp(otp.code) },
                label = {
                    Text(
                        text = "Kodu kopyala ${otp.code}",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                colors =
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        }
    }
}
