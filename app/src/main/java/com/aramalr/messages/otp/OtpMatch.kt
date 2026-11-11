package com.aramalr.messages.otp

data class OtpMatch(
    val code: String,
    val startIndex: Int,
    val endIndex: Int,
    val confidence: Float,
)
