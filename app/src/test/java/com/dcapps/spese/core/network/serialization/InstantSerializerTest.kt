package com.dcapps.spese.core.network.serialization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class InstantSerializerTest {

    @Test
    fun `parses RFC 1123`() {
        // Formato di created_at/updated_at/joined_at dal backend.
        val parsed = InstantSerializer.parse("Tue, 03 Nov 2026 10:00:00 GMT")
        assertEquals(Instant.parse("2026-11-03T10:00:00Z"), parsed)
    }

    @Test
    fun `parses ISO instant with Z`() {
        assertEquals(Instant.parse("2026-11-03T10:00:00Z"), InstantSerializer.parse("2026-11-03T10:00:00Z"))
    }

    @Test
    fun `parses ISO local date-time without offset as UTC`() {
        assertEquals(Instant.parse("2026-11-03T10:00:00Z"), InstantSerializer.parse("2026-11-03T10:00:00"))
    }

    @Test
    fun `parses simple date as start of day UTC`() {
        // purchased_at / expense_date arrivano come "yyyy-MM-dd".
        assertEquals(Instant.parse("2026-11-03T00:00:00Z"), InstantSerializer.parse("2026-11-03"))
    }

    @Test
    fun `returns null for unparseable input`() {
        assertNull(InstantSerializer.parse("not a date"))
        assertNull(InstantSerializer.parse(""))
    }
}
