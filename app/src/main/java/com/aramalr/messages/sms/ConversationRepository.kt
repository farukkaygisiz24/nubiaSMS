package com.aramalr.messages.sms

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.aramalr.messages.otp.OtpDetector
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

class ConversationRepository(private val context: Context) {
    private val resolver: ContentResolver get() = context.contentResolver

    fun observeConversations(): Flow<List<Conversation>> =
        observeUri(Telephony.Sms.CONTENT_URI).map { loadConversations() }

    fun observeMessages(threadId: Long): Flow<List<Message>> =
        observeUri(Telephony.Sms.CONTENT_URI).map { loadMessages(threadId) }

    fun getOrCreateThreadId(address: String): Long =
        Telephony.Threads.getOrCreateThreadId(context, address)

    fun hasReadPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private fun observeUri(uri: Uri): Flow<Unit> =
        callbackFlow {
            trySend(Unit)
            val observer =
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        trySend(Unit)
                    }
                }
            resolver.registerContentObserver(uri, true, observer)
            awaitClose { resolver.unregisterContentObserver(observer) }
        }

    private fun loadConversations(): List<Conversation> {
        if (!hasReadPermission()) return emptyList()
        val latestByThread = LinkedHashMap<Long, Conversation>()
        val projection =
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ,
            )
        resolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC",
        )?.use { cursor ->
            val threadIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val readIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
            while (cursor.moveToNext()) {
                val threadId = cursor.getLong(threadIdx)
                val address = cursor.getString(addressIdx) ?: ""
                val body = cursor.getString(bodyIdx) ?: ""
                val date = cursor.getLong(dateIdx)
                val unread = if (cursor.getInt(readIdx) == 0) 1 else 0
                val existing = latestByThread[threadId]
                if (existing == null) {
                    latestByThread[threadId] =
                        Conversation(
                            threadId = threadId,
                            address = address,
                            displayName = lookupDisplayName(address),
                            snippet = body,
                            dateMillis = date,
                            unreadCount = unread,
                        )
                } else if (unread == 1) {
                    latestByThread[threadId] = existing.copy(unreadCount = existing.unreadCount + 1)
                }
            }
        }
        return latestByThread.values.toList()
    }

    private fun loadMessages(threadId: Long): List<Message> {
        if (!hasReadPermission()) return emptyList()
        val messages = mutableListOf<Message>()
        val projection =
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
            )
        resolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            "${Telephony.Sms.THREAD_ID}=?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} ASC",
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            while (cursor.moveToNext()) {
                val body = cursor.getString(bodyIdx) ?: ""
                val type = cursor.getInt(typeIdx)
                messages +=
                    Message(
                        id = cursor.getLong(idIdx),
                        threadId = cursor.getLong(threadIdx),
                        address = cursor.getString(addressIdx) ?: "",
                        body = body,
                        dateMillis = cursor.getLong(dateIdx),
                        isOutgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT ||
                            type == Telephony.Sms.MESSAGE_TYPE_OUTBOX,
                        otp = OtpDetector.detect(body),
                    )
            }
        }
        return messages
    }

    private fun lookupDisplayName(address: String): String? {
        if (address.isBlank()) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val uri =
            Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(address),
            )
        resolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }
}
