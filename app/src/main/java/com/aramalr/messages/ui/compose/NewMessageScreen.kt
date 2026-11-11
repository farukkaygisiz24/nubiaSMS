package com.aramalr.messages.ui.compose

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aramalr.messages.sms.ConversationRepository

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

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
            Text(
                text = "Yeni mesaj",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            label = { Text("Alıcı") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp),
            placeholder = { Text("Mesaj") },
            shape = RoundedCornerShape(20.dp),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    val to = address.trim()
                    val text = body.trim()
                    if (to.isNotEmpty() && text.isNotEmpty()) {
                        val threadId = repo.getOrCreateThreadId(to)
                        onSend(to, text, threadId)
                    }
                },
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder")
            }
        }
    }
}
