package com.aramalr.messages.sms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsAddressTest {
    @Test
    fun alphanumericSenderRejectsReplies() {
        assertFalse(SmsAddress.acceptsReplies("MEDIAMARKT"))
        assertFalse(SmsAddress.acceptsReplies("TRENDYOL GO"))
    }

    @Test
    fun phoneNumberAcceptsReplies() {
        assertTrue(SmsAddress.acceptsReplies("+905551112233"))
        assertTrue(SmsAddress.acceptsReplies("05551112233"))
    }
}
