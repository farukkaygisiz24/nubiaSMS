package com.nubiasms.messages.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateJsonParserTest {
    @Test
    fun parse_valid() {
        assertEquals(
            4 to "0.1.3",
            UpdateJsonParser.parse("""{"versionCode":4,"versionName":"0.1.3"}"""),
        )
    }

    @Test
    fun parse_invalid() {
        assertNull(UpdateJsonParser.parse("""{}"""))
        assertNull(UpdateJsonParser.parse("not-json"))
    }
}
