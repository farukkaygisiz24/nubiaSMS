package com.nubiasms.messages.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatePolicyTest {
    @Test
    fun kind_noneWhenEqualOrBehind() {
        assertEquals(UpdatePromptKind.None, UpdatePolicy.kind(5, 5))
        assertEquals(UpdatePromptKind.None, UpdatePolicy.kind(5, 4))
    }

    @Test
    fun kind_softWhenOneBehind() {
        assertEquals(UpdatePromptKind.Soft, UpdatePolicy.kind(4, 5))
    }

    @Test
    fun kind_insistentWhenTwoOrMoreBehind() {
        assertEquals(UpdatePromptKind.Insistent, UpdatePolicy.kind(3, 5))
        assertEquals(UpdatePromptKind.Insistent, UpdatePolicy.kind(2, 5))
    }
}
