package com.aramalr.messages.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpDetectorTest {
    @Test
    fun detectsKeywordAnchoredSixDigitCode() {
        val m = OtpDetector.detect("DogruBank kodunuz: 482193. Paylasmayin.")
        assertEquals("482193", m!!.code)
    }

    @Test
    fun prefersCodeNearOtpKeywordsOverBareAmount() {
        val m = OtpDetector.detect("Odeme 1.250,00 TL. Dogrulama kodu 904421 gecerlidir.")
        assertEquals("904421", m!!.code)
    }

    @Test
    fun returnsNullWhenNoCode() {
        assertNull(OtpDetector.detect("Yarin saat 15:00 randevun var."))
    }
}
