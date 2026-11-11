package com.aramalr.messages.sms

object SmsAddress {
    /**
     * Alphanumeric originators (e.g. MEDIAMARKT) typically cannot receive SMS replies.
     * Phone-number addresses can.
     */
    fun acceptsReplies(address: String): Boolean {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.any { it.isLetter() }) return false
        return trimmed.any { it.isDigit() }
    }
}
