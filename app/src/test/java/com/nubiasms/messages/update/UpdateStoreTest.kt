package com.nubiasms.messages.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateStoreTest {
    @Test
    fun displayKind_softDismissSuppressesSoftOnly() {
        assertEquals(
            UpdatePromptKind.None,
            UpdatePolicy.displayKind(UpdatePromptKind.Soft, latestVersionCode = 5, softDismissedForVersionCode = 5),
        )
        assertEquals(
            UpdatePromptKind.Insistent,
            UpdatePolicy.displayKind(UpdatePromptKind.Insistent, latestVersionCode = 5, softDismissedForVersionCode = 5),
        )
        assertEquals(
            UpdatePromptKind.Soft,
            UpdatePolicy.displayKind(UpdatePromptKind.Soft, latestVersionCode = 5, softDismissedForVersionCode = 0),
        )
    }
}
