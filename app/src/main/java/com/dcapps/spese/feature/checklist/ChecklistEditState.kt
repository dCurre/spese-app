package com.dcapps.spese.feature.checklist

import com.dcapps.spese.core.model.BatchCategoryCreate
import com.dcapps.spese.core.model.BatchCategoryUpdate
import com.dcapps.spese.core.model.BatchItemCreate
import com.dcapps.spese.core.model.BatchItemUpdate
import com.dcapps.spese.core.model.BatchSaveRequest
import com.dcapps.spese.core.model.ShoppingList

/**
 * Stato di modifica locale della checklist: una copia editabile dell'albero che accumula le
 * modifiche finché l'utente non salva. Al salvataggio produce un [BatchSaveRequest] confrontando
 * lo stato corrente con quello originale (diff).
 *
 * Gli elementi nuovi hanno id negativo temporaneo; nel batch diventano `temp_id` stringa.
 */
data class EditItem(
    val id: Long,            // > 0 = esistente, < 0 = nuovo (temporaneo)
    val name: String,
    val quantity: Int,
    val checked: Boolean,
    val categoryId: Long?,   // id reale o id temporaneo (< 0) della categoria, null = nessuna
    val sortOrder: Int,
)

data class EditCategory(
    val id: Long,            // > 0 = esistente, < 0 = nuova (temporanea)
    val name: String,
    val parentId: Long?,
    val sortOrder: Int,
)

class ChecklistEditState private constructor(
    val items: List<EditItem>,
    val categories: List<EditCategory>,
    private val originalItems: Map<Long, EditItem>,
    private val originalCategories: Map<Long, EditCategory>,
    private val nextTempId: Long,
) {
    val hasChanges: Boolean
        get() = items != originalItems.values.toList() || categories != originalCategories.values.toList() ||
            items.any { it.id < 0 } || categories.any { it.id < 0 } ||
            originalItems.keys.any { id -> items.none { it.id == id } } ||
            originalCategories.keys.any { id -> categories.none { it.id == id } }

    fun withItems(newItems: List<EditItem>) =
        ChecklistEditState(newItems, categories, originalItems, originalCategories, nextTempId)

    fun withCategories(newCats: List<EditCategory>) =
        ChecklistEditState(items, newCats, originalItems, originalCategories, nextTempId)

    fun addItem(name: String, categoryId: Long?): ChecklistEditState {
        val temp = -(nextTempId)
        val newItem = EditItem(temp, name.trim(), 1, false, categoryId, items.size)
        return ChecklistEditState(
            items + newItem, categories, originalItems, originalCategories, nextTempId + 1,
        )
    }

    fun addCategory(name: String, parentId: Long?): ChecklistEditState {
        val temp = -(nextTempId)
        val newCat = EditCategory(temp, name.trim(), parentId, categories.size)
        return ChecklistEditState(
            items, categories + newCat, originalItems, originalCategories, nextTempId + 1,
        )
    }

    fun updateItem(id: Long, transform: (EditItem) -> EditItem) =
        withItems(items.map { if (it.id == id) transform(it) else it })

    fun removeItem(id: Long) = withItems(items.filterNot { it.id == id })

    fun updateCategory(id: Long, transform: (EditCategory) -> EditCategory) =
        withCategories(categories.map { if (it.id == id) transform(it) else it })

    fun removeCategory(id: Long): ChecklistEditState {
        // Rimuove la categoria e stacca gli item che vi appartenevano.
        val newItems = items.map { if (it.categoryId == id) it.copy(categoryId = null) else it }
        return ChecklistEditState(
            newItems, categories.filterNot { it.id == id }, originalItems, originalCategories, nextTempId,
        )
    }

    /** Calcola il payload batch confrontando lo stato corrente con l'originale. */
    fun toBatchRequest(): BatchSaveRequest {
        fun tempId(id: Long) = "t${-id}"

        val catsCreate = categories.filter { it.id < 0 }.map { c ->
            BatchCategoryCreate(
                tempId = tempId(c.id),
                name = c.name,
                parentId = c.parentId?.let { if (it < 0) tempId(it) else it.toString() },
                sortOrder = c.sortOrder,
            )
        }
        val catsUpdate = categories.filter { it.id > 0 }.mapNotNull { c ->
            val orig = originalCategories[c.id]
            if (orig != null && orig.name != c.name) BatchCategoryUpdate(c.id, c.name) else null
        }
        val catsDelete = originalCategories.keys.filter { id -> categories.none { it.id == id } }

        val itemsCreate = items.filter { it.id < 0 }.map { i ->
            BatchItemCreate(
                name = i.name,
                quantity = i.quantity,
                checked = i.checked,
                sortOrder = i.sortOrder,
                categoryId = i.categoryId?.takeIf { it > 0 },
                categoryTempId = i.categoryId?.let { if (it < 0) tempId(it) else null },
            )
        }
        val itemsUpdate = items.filter { it.id > 0 }.mapNotNull { i ->
            val orig = originalItems[i.id] ?: return@mapNotNull null
            if (orig != i) {
                BatchItemUpdate(
                    id = i.id,
                    name = i.name.takeIf { it != orig.name },
                    quantity = i.quantity.takeIf { it != orig.quantity },
                    checked = i.checked.takeIf { it != orig.checked },
                    categoryId = i.categoryId?.takeIf { it != orig.categoryId && it > 0 },
                )
            } else null
        }
        val itemsDelete = originalItems.keys.filter { id -> items.none { it.id == id } }

        return BatchSaveRequest(
            categoriesUpdate = catsUpdate,
            categoriesDelete = catsDelete,
            categoriesCreate = catsCreate,
            itemsUpdate = itemsUpdate,
            itemsDelete = itemsDelete,
            itemsCreate = itemsCreate,
        )
    }

    companion object {
        /** Costruisce lo stato di modifica dall'albero caricato. */
        fun from(list: ShoppingList): ChecklistEditState {
            val items = mutableListOf<EditItem>()
            val cats = mutableListOf<EditCategory>()

            list.items.forEach { i ->
                items += EditItem(i.id, i.name, i.quantity ?: 1, i.checked, null, i.sortOrder)
            }
            fun walk(categories: List<com.dcapps.spese.core.model.ShoppingCategory>) {
                categories.forEach { c ->
                    cats += EditCategory(c.id, c.name, c.parentId, c.sortOrder)
                    c.items.forEach { i ->
                        items += EditItem(i.id, i.name, i.quantity ?: 1, i.checked, c.id, i.sortOrder)
                    }
                    walk(c.children)
                }
            }
            walk(list.categories)

            return ChecklistEditState(
                items = items,
                categories = cats,
                originalItems = items.associateBy { it.id },
                originalCategories = cats.associateBy { it.id },
                nextTempId = 1,
            )
        }
    }
}
