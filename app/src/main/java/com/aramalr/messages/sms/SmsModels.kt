package com.aramalr.messages.sms

import com.aramalr.messages.otp.OtpMatch

data class Conversation(
    val threadId: Long,
    val address: String,
    val displayName: String?,
    val snippet: String,
    val dateMillis: Long,
    val unreadCount: Int,
)

data class Message(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val dateMillis: Long,
    val isOutgoing: Boolean,
    val otp: OtpMatch?,
)
