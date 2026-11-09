package com.dcapps.spese.feature.checklist

import com.dcapps.spese.core.model.ShoppingCategory
import com.dcapps.spese.core.model.ShoppingItem
import com.dcapps.spese.core.model.ShoppingList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistEditStateTest {

    private fun item(id: Long, name: String, checked: Boolean = false, catId: Long? = null) =
        ShoppingItem(id = id, shoppingListId = 1, categoryId = catId, name = name, quantity = 1, checked = checked)

    private fun list(
        items: List<ShoppingItem> = emptyList(),
        categories: List<ShoppingCategory> = emptyList(),
    ) = ShoppingList(id = 1, name = "L", ownerId = 1, items = items, categories = categories)

    @Test
    fun `stato appena creato non ha modifiche`() {
        val s = ChecklistEditState.from(list(items = listOf(item(1, "Mele"))))
        assertFalse(s.hasChanges)
    }

    @Test
    fun `aggiunta item produce un create e segnala modifiche`() {
        val s = ChecklistEditState.from(list(items = listOf(item(1, "Mele"))))
            .addItem("Pane", categoryId = null)
        assertTrue(s.hasChanges)
        val req = s.toBatchRequest()
        assertEquals(1, req.itemsCreate.size)
        assertEquals("Pane", req.itemsCreate[0].name)
        assertTrue(req.itemsUpdate.isEmpty())
        assertTrue(req.itemsDelete.isEmpty())
    }

    @Test
    fun `modifica nome item produce un update solo sul campo cambiato`() {
        val s = ChecklistEditState.from(list(items = listOf(item(1, "Mele"))))
            .updateItem(1) { it.copy(name = "Mele rosse") }
        val req = s.toBatchRequest()
        assertEquals(1, req.itemsUpdate.size)
        assertEquals(1L, req.itemsUpdate[0].id)
        assertEquals("Mele rosse", req.itemsUpdate[0].name)
        // La quantità non è cambiata => resta null nel diff.
        assertEquals(null, req.itemsUpdate[0].quantity)
    }

    @Test
    fun `rimozione item produce un delete`() {
        val s = ChecklistEditState.from(list(items = listOf(item(1, "Mele"), item(2, "Pane"))))
            .removeItem(2)
        val req = s.toBatchRequest()
        assertEquals(listOf(2L), req.itemsDelete)
    }

    @Test
    fun `nuova categoria con nuovo item collega l'item via temp id`() {
        var s = ChecklistEditState.from(list())
        s = s.addCategory("Frutta", parentId = null)
        val tempCatId = s.categories.first { it.id < 0 }.id
        s = s.addItem("Mele", categoryId = tempCatId)
        val req = s.toBatchRequest()
        assertEquals(1, req.categoriesCreate.size)
        assertEquals(1, req.itemsCreate.size)
        // L'item create riferisce la categoria nuova via category_temp_id, non category_id.
        assertEquals(req.categoriesCreate[0].tempId, req.itemsCreate[0].categoryTempId)
        assertEquals(null, req.itemsCreate[0].categoryId)
    }

    @Test
    fun `rimozione categoria stacca gli item e li lascia senza categoria`() {
        val cat = ShoppingCategory(id = 5, shoppingListId = 1, name = "Frutta", items = listOf(item(1, "Mele", catId = 5)))
        var s = ChecklistEditState.from(list(categories = listOf(cat)))
        s = s.removeCategory(5)
        val req = s.toBatchRequest()
        assertEquals(listOf(5L), req.categoriesDelete)
        // L'item resta (non cancellato) ma spostato fuori dalla categoria.
        assertTrue(req.itemsDelete.isEmpty())
    }
}
