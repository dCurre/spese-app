package com.dcapps.spese.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogTest {

    private val sample = """
        # Novità

        ## 0.1.5
        - Prima novità
        - Seconda novità

        ## 0.1.4
        - Qualcosa

        ## 0.1.3
        - Vecchia novità
    """.trimIndent()

    @Test
    fun `parse estrae tutte le sezioni con le loro righe`() {
        val entries = Changelog.parse(sample)
        assertEquals(3, entries.size)
        assertEquals(SemVer(0, 1, 5), entries[0].version)
        assertEquals(listOf("Prima novità", "Seconda novità"), entries[0].lines)
        assertEquals(listOf("Qualcosa"), entries[1].lines)
    }

    @Test
    fun `entriesInRange esclude la versione installata e include la disponibile`() {
        // Da 0.1.3 (installata, esclusa) a 0.1.5 (disponibile, inclusa) => 0.1.5 e 0.1.4.
        val range = Changelog.entriesInRange(sample, from = SemVer(0, 1, 3), to = SemVer(0, 1, 5))
        assertEquals(listOf(SemVer(0, 1, 5), SemVer(0, 1, 4)), range.map { it.version })
    }

    @Test
    fun `entriesInRange ordina dalla piu recente alla piu vecchia`() {
        val range = Changelog.entriesInRange(sample, from = null, to = SemVer(0, 1, 5))
        assertEquals(listOf(SemVer(0, 1, 5), SemVer(0, 1, 4), SemVer(0, 1, 3)), range.map { it.version })
    }

    @Test
    fun `entriesInRange vuoto se non ci sono versioni nel range`() {
        val range = Changelog.entriesInRange(sample, from = SemVer(0, 1, 5), to = SemVer(0, 1, 5))
        assertTrue(range.isEmpty())
    }

    @Test
    fun `parse ignora testo prima della prima sezione e righe vuote`() {
        val entries = Changelog.parse("blah blah\n\n## 1.0.0\n- Uno\n")
        assertEquals(1, entries.size)
        assertEquals(listOf("Uno"), entries[0].lines)
    }
}
