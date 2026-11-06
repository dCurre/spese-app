package com.dcapps.spese.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {

    @Test
    fun `parse full version`() {
        assertEquals(SemVer(1, 2, 3), SemVer.parse("1.2.3"))
    }

    @Test
    fun `parse strips v prefix (lower and upper)`() {
        assertEquals(SemVer(1, 2, 3), SemVer.parse("v1.2.3"))
        assertEquals(SemVer(1, 2, 3), SemVer.parse("V1.2.3"))
    }

    @Test
    fun `parse trims whitespace`() {
        assertEquals(SemVer(0, 0, 25), SemVer.parse("  0.0.25  "))
    }

    @Test
    fun `missing components default to zero`() {
        assertEquals(SemVer(1, 0, 0), SemVer.parse("1"))
        assertEquals(SemVer(1, 2, 0), SemVer.parse("1.2"))
    }

    @Test
    fun `null or blank or invalid major returns null`() {
        assertNull(SemVer.parse(null))
        assertNull(SemVer.parse(""))
        assertNull(SemVer.parse("   "))
        assertNull(SemVer.parse("abc"))
        assertNull(SemVer.parse("v.1.2"))
    }

    @Test
    fun `numeric comparison beats string comparison`() {
        // "0.0.10" > "0.0.9": il confronto di stringhe fallirebbe, quello numerico no.
        assertTrue(SemVer.parse("0.0.10")!! > SemVer.parse("0.0.9")!!)
    }

    @Test
    fun `comparison across components`() {
        assertTrue(SemVer(1, 0, 0) > SemVer(0, 9, 9))
        assertTrue(SemVer(1, 2, 0) > SemVer(1, 1, 99))
        assertEquals(0, SemVer(1, 2, 3).compareTo(SemVer(1, 2, 3)))
    }

    @Test
    fun `toString round-trips`() {
        assertEquals("0.0.29", SemVer(0, 0, 29).toString())
        assertEquals(SemVer(0, 0, 29), SemVer.parse(SemVer(0, 0, 29).toString()))
    }
}
