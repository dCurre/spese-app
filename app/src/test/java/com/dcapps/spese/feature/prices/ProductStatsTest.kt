package com.dcapps.spese.feature.prices

import com.dcapps.spese.core.model.Purchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ProductStatsTest {

    private fun purchase(
        id: Long,
        date: String,
        unitPrice: Double,
        store: String? = null,
    ) = Purchase(
        id = id, productId = 1L, userId = 1L, purchasedAt = date,
        unitPrice = unitPrice, store = store,
    )

    @Test
    fun `nessun acquisto restituisce null`() {
        assertNull(productStats(emptyList()))
    }

    @Test
    fun `un solo acquisto un solo acquisto min e last coincidono`() {
        val s = productStats(listOf(purchase(1, "2026-01-01", 2.50, store = "Esselunga")))!!
        assertEquals(2.50, s.minUnitPrice, 0.0001)
        assertEquals(2.50, s.lastUnitPrice, 0.0001)
        assertEquals("Esselunga", s.minStore)
        assertEquals(0.0, s.variationPct, 0.0001)
        assertTrue(s.isAtMinimum)
    }

    @Test
    fun `ultimo prezzo sopra il minimo variazione positiva`() {
        val s = productStats(
            listOf(
                purchase(1, "2026-01-01", 2.00), // minimo
                purchase(2, "2026-03-01", 2.50), // ultimo (data max)
            ),
        )!!
        assertEquals(2.00, s.minUnitPrice, 0.0001)
        assertEquals(2.50, s.lastUnitPrice, 0.0001)
        assertEquals(25.0, s.variationPct, 0.0001) // (2.50-2.00)/2.00*100
        assertFalse(s.isAtMinimum)
    }

    @Test
    fun `ultimo prezzo e' il minimo ultimo e il minimo con piu acquisti`() {
        val s = productStats(
            listOf(
                purchase(1, "2026-01-01", 3.00),
                purchase(2, "2026-02-01", 2.50),
                purchase(3, "2026-03-01", 2.00), // ultimo E minimo
            ),
        )!!
        assertEquals(2.00, s.minUnitPrice, 0.0001)
        assertEquals(2.00, s.lastUnitPrice, 0.0001)
        assertTrue(s.isAtMinimum)
        assertEquals(0.0, s.variationPct, 0.0001)
    }

    @Test
    fun `ultimo determinato dalla data, non dall'ordine lista`() {
        // L'acquisto più recente (data max) non è l'ultimo nella lista.
        val s = productStats(
            listOf(
                purchase(1, "2026-05-01", 4.00), // data max -> ultimo
                purchase(2, "2026-01-01", 2.00), // minimo
            ),
        )!!
        assertEquals(2.00, s.minUnitPrice, 0.0001)
        assertEquals(4.00, s.lastUnitPrice, 0.0001)
        assertEquals(100.0, s.variationPct, 0.0001)
    }

    @Test
    fun `minStore e' quello dell'acquisto al prezzo minimo`() {
        val s = productStats(
            listOf(
                purchase(1, "2026-01-01", 3.00, store = "Coop"),
                purchase(2, "2026-02-01", 1.50, store = "Lidl"), // minimo
            ),
        )!!
        assertEquals("Lidl", s.minStore)
    }

    @Test
    fun `store vuoto del minimo da minStore null`() {
        val s = productStats(listOf(purchase(1, "2026-01-01", 1.0, store = "  ")))!!
        assertNull(s.minStore)
    }
}
