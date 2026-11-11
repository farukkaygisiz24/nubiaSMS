package com.aramalr.messages.evidence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OtpEvidenceLogTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun appendThenReadAllReturnsEvent() {
        val log = OtpEvidenceLog(tmp.newFolder("evidence"))
        val event =
            OtpEvidenceEvent(
                timestampMillis = 1_700_000_000_000L,
                sender = "+905551112233",
                body = "kodunuz: 123456",
                detectedCode = "123456",
                confidence = 0.9f,
                assistPath = "none",
            )
        log.append(event)
        val all = log.readAll()
        assertEquals(1, all.size)
        assertEquals("123456", all[0].detectedCode)
        assertEquals("none", all[0].assistPath)
        assertEquals("+905551112233", all[0].sender)
    }

    @Test
    fun clearEmptiesLog() {
        val log = OtpEvidenceLog(tmp.newFolder("evidence2"))
        log.append(
            OtpEvidenceEvent(
                timestampMillis = 1L,
                sender = "BANK",
                body = "x",
                detectedCode = null,
                confidence = null,
                assistPath = "none",
            ),
        )
        log.clear()
        assertTrue(log.readAll().isEmpty())
    }
}
