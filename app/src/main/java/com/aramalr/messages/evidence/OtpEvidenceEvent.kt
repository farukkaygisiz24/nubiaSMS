package com.aramalr.messages.evidence

data class OtpEvidenceEvent(
    val timestampMillis: Long,
    val sender: String,
    val body: String,
    val detectedCode: String?,
    val confidence: Float?,
    val assistPath: String,
    val notes: String = "",
)
