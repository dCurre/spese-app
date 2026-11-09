package com.dcapps.spese.core.update

/** Note di una singola versione nel changelog. */
data class ChangelogEntry(
    val version: SemVer,
    val lines: List<String>,
)

/**
 * Parser del CHANGELOG.md "parlante" (fonte unica delle note di rilascio per l'utente).
 * Il file ha sezioni `## X.Y.Z` seguite da righe `- ...`. Qui si estraggono le sezioni e si
 * filtrano per intervallo di versione, così la schermata di aggiornamento può mostrare SOLO le
 * novità delle versioni che l'utente sta per installare (installata esclusa → disponibile inclusa).
 */
object Changelog {

    private val HEADER = Regex("""^##\s+v?(\d+\.\d+\.\d+)\s*$""")

    /** Spezza il testo del CHANGELOG in sezioni per versione. */
    fun parse(markdown: String): List<ChangelogEntry> {
        val entries = mutableListOf<ChangelogEntry>()
        var currentVer: SemVer? = null
        val currentLines = mutableListOf<String>()

        fun flush() {
            val v = currentVer
            if (v != null && currentLines.isNotEmpty()) {
                entries += ChangelogEntry(v, currentLines.toList())
            }
            currentLines.clear()
        }

        markdown.lineSequence().forEach { raw ->
            val line = raw.trimEnd()
            val m = HEADER.find(line.trim())
            if (m != null) {
                flush()
                currentVer = SemVer.parse(m.groupValues[1])
            } else if (currentVer != null) {
                // Righe di contenuto: tiene le voci dell'elenco (togliendo il "- " iniziale) e salta
                // le righe vuote.
                val t = line.trim()
                if (t.isNotEmpty()) {
                    currentLines += t.removePrefix("- ").removePrefix("* ").trim()
                }
            }
        }
        flush()
        return entries
    }

    /**
     * Novità da mostrare passando da [from] (installata, ESCLUSA) a [to] (disponibile, INCLUSA),
     * ordinate dalla più recente alla più vecchia. Se [from] è null, mostra tutto fino a [to].
     */
    fun entriesInRange(markdown: String, from: SemVer?, to: SemVer): List<ChangelogEntry> =
        parse(markdown)
            .filter { (from == null || it.version > from) && it.version <= to }
            .sortedByDescending { it.version }
}
