package com.dcapps.spese.feature.checklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistImportTest {

    @Test
    fun `testo vuoto o solo spazi torna null`() {
        assertNull(ChecklistImport.parse(""))
        assertNull(ChecklistImport.parse("   \n  \n"))
    }

    @Test
    fun `righe semplici diventano articoli non spuntati`() {
        val req = ChecklistImport.parse("Mele\nPane\nLatte")!!
        assertEquals(3, req.itemsCreate.size)
        assertEquals(listOf("Mele", "Pane", "Latte"), req.itemsCreate.map { it.name })
        assertTrue(req.itemsCreate.none { it.checked })
    }

    @Test
    fun `marcatori di spunta riconosciuti`() {
        val req = ChecklistImport.parse("[x] Fatto\n[ ] Da fare\n✓ Spuntato\n○ Aperto\n- Trattino")!!
        val byName = req.itemsCreate.associateBy { it.name }
        assertTrue(byName["Fatto"]!!.checked)
        assertFalse(byName["Da fare"]!!.checked)
        assertTrue(byName["Spuntato"]!!.checked)
        assertFalse(byName["Aperto"]!!.checked)
        assertFalse(byName["Trattino"]!!.checked)
    }

    @Test
    fun `quantita in coda estratta con x e per`() {
        val req = ChecklistImport.parse("Mele x3\nPane ×2\nLatte")!!
        val byName = req.itemsCreate.associateBy { it.name }
        assertEquals(3, byName["Mele"]!!.quantity)
        assertEquals(2, byName["Pane"]!!.quantity)
        assertEquals(1, byName["Latte"]!!.quantity)
    }

    @Test
    fun `riga con due punti crea categoria e assegna gli articoli seguenti`() {
        val req = ChecklistImport.parse("Frutta:\nMele\nPere\nPane")!!
        assertEquals(1, req.categoriesCreate.size)
        val catTemp = req.categoriesCreate[0].tempId
        val mele = req.itemsCreate.first { it.name == "Mele" }
        val pere = req.itemsCreate.first { it.name == "Pere" }
        val pane = req.itemsCreate.first { it.name == "Pane" }
        assertEquals(catTemp, mele.categoryTempId)
        assertEquals(catTemp, pere.categoryTempId)
        // "Pane" segue la stessa categoria perché non c'è un nuovo header: resta in Frutta.
        assertEquals(catTemp, pane.categoryTempId)
    }

    @Test
    fun `articoli prima di ogni categoria restano senza categoria`() {
        val req = ChecklistImport.parse("Senza\nFrutta:\nMele")!!
        assertNull(req.itemsCreate.first { it.name == "Senza" }.categoryTempId)
        assertEquals(req.categoriesCreate[0].tempId, req.itemsCreate.first { it.name == "Mele" }.categoryTempId)
    }
}
