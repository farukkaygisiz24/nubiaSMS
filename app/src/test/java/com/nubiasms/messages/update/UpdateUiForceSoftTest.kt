package com.nubiasms.messages.update

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Documents UpdatePromptHost contract: resume must not permanently force Soft
 * (manual check uses force once; resume uses softDismissed).
 */
class UpdateUiForceSoftTest {
    @Test
    fun resumeRespectsSoftDismiss_manualForceBypasses() {
        assertEquals(
            UpdatePromptKind.None,
            UpdatePolicy.displayKind(UpdatePromptKind.Soft, 5, softDismissedForVersionCode = 5),
        )
        assertEquals(
            UpdatePromptKind.Soft,
            UpdatePolicy.displayKind(UpdatePromptKind.Soft, 5, softDismissedForVersionCode = 0),
        )
    }
}
