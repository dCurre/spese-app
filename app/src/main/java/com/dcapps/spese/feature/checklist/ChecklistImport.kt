package com.dcapps.spese.feature.checklist

import com.dcapps.spese.core.model.BatchCategoryCreate
import com.dcapps.spese.core.model.BatchItemCreate
import com.dcapps.spese.core.model.BatchSaveRequest

/**
 * Parsing PURO del testo importato in una checklist (nessuna dipendenza da ViewModel/rete, così è
 * testabile a unità). Riconosce:
 *  - riga che termina con ":" → categoria (gli articoli seguenti le vengono assegnati);
 *  - marcatori di spunta a inizio riga: Samsung Notes `[v]`/`[x]`/`[X]` (spuntato), `[ ]` (non
 *    spuntato); export app `✓` (spuntato), `○` (non spuntato); trattino `-` o testo semplice;
 *  - quantità opzionale in coda come " x3" o " ×3".
 */
object ChecklistImport {

    private val CHECKED_PREFIXES = listOf("[v]", "[x]", "[X]", "✓", "☑", "[✓]")
    private val UNCHECKED_PREFIXES = listOf("[ ]", "○", "☐", "-", "[]")
    private val QTY_SUFFIX = Regex("""\s[x×](\d+)$""")

    /**
     * Converte [text] in una [BatchSaveRequest] con categorie e articoli da creare, o null se non
     * c'è nulla di valido da importare.
     */
    fun parse(text: String): BatchSaveRequest? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        var tempId = 1L
        val catsCreate = mutableListOf<BatchCategoryCreate>()
        val itemsCreate = mutableListOf<BatchItemCreate>()
        var currentCatTemp: String? = null

        lines.forEach { raw ->
            if (raw.endsWith(":")) {
                val name = raw.dropLast(1).trim()
                if (name.isNotEmpty()) {
                    val t = "t${tempId++}"
                    catsCreate += BatchCategoryCreate(tempId = t, name = name)
                    currentCatTemp = t
                }
            } else {
                var checked = false
                var body = raw
                CHECKED_PREFIXES.firstOrNull { body.startsWith(it) }?.let {
                    checked = true; body = body.removePrefix(it).trim()
                }
                if (!checked) {
                    UNCHECKED_PREFIXES.firstOrNull { body.startsWith(it) }?.let {
                        body = body.removePrefix(it).trim()
                    }
                }
                var quantity = 1
                QTY_SUFFIX.find(body)?.let { m ->
                    quantity = m.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
                    body = body.removeRange(m.range).trim()
                }
                val name = body.trim()
                if (name.isNotEmpty()) {
                    itemsCreate += BatchItemCreate(
                        name = name,
                        quantity = quantity,
                        checked = checked,
                        categoryTempId = currentCatTemp,
                    )
                }
            }
        }
        if (catsCreate.isEmpty() && itemsCreate.isEmpty()) return null
        return BatchSaveRequest(categoriesCreate = catsCreate, itemsCreate = itemsCreate)
    }
}
