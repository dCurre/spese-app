package com.dcapps.spese.core.local

import com.dcapps.spese.core.local.dao.ChecklistDao
import com.dcapps.spese.core.local.entity.LocalShoppingCategoryEntity
import com.dcapps.spese.core.local.entity.LocalShoppingItemEntity
import com.dcapps.spese.core.local.entity.LocalShoppingListEntity
import com.dcapps.spese.core.model.BatchSaveRequest
import com.dcapps.spese.core.model.ShoppingCategory
import com.dcapps.spese.core.model.ShoppingItem
import com.dcapps.spese.core.model.ShoppingList
import com.dcapps.spese.core.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sorgente dati LOCALE (Room) local-first delle checklist. Le letture per la UI sono Flow reattivi
 * (si aggiornano quando Room cambia, anche a seguito di sync). Le scritture aggiornano Room marcando
 * `pendingSync`/`updatedAt`; la cancellazione è un tombstone (`deleted = true`), non una DELETE
 * fisica (la rimozione vera avviene dopo che il SyncEngine ha propagato). L'albero categorie/articoli
 * viene ricomposto come fa il backend.
 */
@Singleton
class ChecklistLocalDataSource @Inject constructor(
    private val dao: ChecklistDao,
) {
    private fun now() = System.currentTimeMillis()

    // --- Letture reattive (Flow) ---

    /** Tutte le liste con conteggi articoli/spuntati (senza l'albero completo). */
    fun observeLists(): Flow<List<ShoppingList>> =
        // Combina le liste con il flusso di tutti gli item: così i conteggi si rinfrescano quando
        // un articolo cambia (aggiunto/spuntato/cancellato), non solo quando cambia la lista.
        dao.observeLists().combine(dao.observeAllItems()) { lists, allItems ->
            lists.map { e ->
                val items = allItems.filter { it.listId == e.id && !it.deleted }
                e.toModel(
                    itemsCount = items.size,
                    checkedCount = items.count { it.checked },
                )
            }
        }

    /** Una lista con l'albero completo (categorie annidate + articoli), reattiva. */
    fun observeList(id: Long): Flow<ShoppingList?> =
        combine(dao.observeList(id), dao.observeCategories(id), dao.observeItems(id)) { listEntity, cats, items ->
            listEntity?.toModel(
                itemsCount = items.size,
                checkedCount = items.count { it.checked },
                categories = buildTree(id, parentId = null, cats, items),
                rootItems = items.filter { it.categoryId == null }.map { it.toModel() },
            )
        }

    // --- Scritture (marcano pendingSync) ---

    suspend fun createList(name: String): ApiResult<Long> = guard {
        dao.insertList(LocalShoppingListEntity(name = name, pendingSync = true, updatedAt = now()))
    }

    suspend fun updateList(id: Long, name: String?, completed: Boolean?, starred: Boolean?): ApiResult<Unit> = guard {
        val e = dao.getList(id) ?: error("Lista non trovata")
        dao.updateList(
            e.copy(
                name = name ?: e.name,
                completed = completed ?: e.completed,
                starred = starred ?: e.starred,
                pendingSync = true,
                updatedAt = now(),
            ),
        )
    }

    suspend fun deleteList(id: Long): ApiResult<Unit> = guard { tombstoneList(id) }

    suspend fun deleteLists(ids: List<Long>): ApiResult<Unit> = guard { ids.forEach { tombstoneList(it) } }

    suspend fun addItem(listId: Long, name: String, categoryId: Long?): ApiResult<Long> = guard {
        dao.insertItem(
            LocalShoppingItemEntity(listId = listId, categoryId = categoryId, name = name, pendingSync = true, updatedAt = now()),
        )
    }

    suspend fun updateItem(id: Long, name: String, quantity: Int, categoryId: Long?): ApiResult<Unit> = guard {
        val cur = dao.getItem(id) ?: error("Articolo non trovato")
        dao.updateItem(cur.copy(name = name, quantity = quantity, categoryId = categoryId, pendingSync = true, updatedAt = now()))
    }

    suspend fun setItemChecked(id: Long, checked: Boolean): ApiResult<Unit> = guard {
        val cur = dao.getItem(id) ?: error("Articolo non trovato")
        dao.updateItem(cur.copy(checked = checked, pendingSync = true, updatedAt = now()))
    }

    suspend fun setAllChecked(listId: Long, checked: Boolean): ApiResult<Unit> = guard {
        dao.getItems(listId).forEach { dao.updateItem(it.copy(checked = checked, pendingSync = true, updatedAt = now())) }
    }

    suspend fun deleteItem(id: Long): ApiResult<Unit> = guard { tombstoneItem(id) }

    suspend fun addCategory(listId: Long, name: String, parentId: Long?): ApiResult<Long> = guard {
        dao.insertCategory(
            LocalShoppingCategoryEntity(listId = listId, name = name, parentId = parentId, pendingSync = true, updatedAt = now()),
        )
    }

    suspend fun updateCategory(id: Long, listId: Long, name: String, parentId: Long?): ApiResult<Unit> = guard {
        val cur = dao.getCategories(listId).firstOrNull { it.id == id }
            ?: LocalShoppingCategoryEntity(id = id, listId = listId, name = name, parentId = parentId)
        dao.updateCategory(cur.copy(name = name, parentId = parentId, pendingSync = true, updatedAt = now()))
    }

    suspend fun deleteCategory(id: Long): ApiResult<Unit> = guard { tombstoneCategory(id) }

    suspend fun listIdOfCategory(categoryId: Long): Long = dao.listIdOfCategory(categoryId) ?: 0L

    /** Applica un BatchSaveRequest su Room (import testo / modifica bulk), rimappando i temp-id. */
    suspend fun applyBatch(listId: Long, body: BatchSaveRequest): ApiResult<Unit> = guard {
        val tempToId = mutableMapOf<String, Long>()
        body.categoriesCreate.forEach { c ->
            val parent = c.parentId?.let { p -> tempToId[p] ?: p.toLongOrNull() }
            val newId = dao.insertCategory(
                LocalShoppingCategoryEntity(listId = listId, name = c.name, parentId = parent, sortOrder = c.sortOrder, pendingSync = true, updatedAt = now()),
            )
            tempToId[c.tempId] = newId
        }
        body.itemsCreate.forEach { i ->
            val catId = i.categoryTempId?.let { tempToId[it] } ?: i.categoryId
            dao.insertItem(
                LocalShoppingItemEntity(
                    listId = listId, categoryId = catId, name = i.name,
                    quantity = i.quantity, checked = i.checked, sortOrder = i.sortOrder,
                    pendingSync = true, updatedAt = now(),
                ),
            )
        }
        body.categoriesUpdate.forEach { u ->
            val cur = dao.getCategories(listId).firstOrNull { it.id == u.id } ?: return@forEach
            dao.updateCategory(cur.copy(name = u.name ?: cur.name, pendingSync = true, updatedAt = now()))
        }
        body.itemsUpdate.forEach { u ->
            val cur = dao.getItem(u.id) ?: return@forEach
            dao.updateItem(
                cur.copy(
                    name = u.name ?: cur.name,
                    quantity = u.quantity ?: cur.quantity,
                    checked = u.checked ?: cur.checked,
                    categoryId = u.categoryId ?: cur.categoryId,
                    pendingSync = true, updatedAt = now(),
                ),
            )
        }
        body.itemsDelete.forEach { tombstoneItem(it) }
        body.categoriesDelete.forEach { tombstoneCategory(it) }
    }

    // --- helper tombstone ---

    private suspend fun tombstoneList(id: Long) {
        val e = dao.getList(id) ?: return
        dao.updateList(e.copy(deleted = true, pendingSync = true, updatedAt = now()))
    }

    private suspend fun tombstoneItem(id: Long) {
        val e = dao.getItem(id) ?: return
        dao.updateItem(e.copy(deleted = true, pendingSync = true, updatedAt = now()))
    }

    private suspend fun tombstoneCategory(id: Long) {
        val listId = dao.listIdOfCategory(id) ?: return
        val e = dao.getCategories(listId).firstOrNull { it.id == id } ?: return
        dao.updateCategory(e.copy(deleted = true, pendingSync = true, updatedAt = now()))
    }

    // --- ricomposizione albero ---

    private fun buildTree(
        listId: Long,
        parentId: Long?,
        cats: List<LocalShoppingCategoryEntity>,
        items: List<LocalShoppingItemEntity>,
    ): List<ShoppingCategory> =
        cats.filter { it.parentId == parentId }.sortedBy { it.sortOrder }.map { c ->
            ShoppingCategory(
                id = c.id,
                shoppingListId = listId,
                parentId = c.parentId,
                name = c.name,
                sortOrder = c.sortOrder,
                items = items.filter { it.categoryId == c.id }.map { it.toModel() },
                children = buildTree(listId, c.id, cats, items),
            )
        }
}

private inline fun <T> guard(block: () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: Exception) {
        ApiResult.Error(message = e.message ?: "Errore locale", cause = e)
    }

private fun LocalShoppingListEntity.toModel(
    itemsCount: Int,
    checkedCount: Int,
    categories: List<ShoppingCategory> = emptyList(),
    rootItems: List<ShoppingItem> = emptyList(),
) = ShoppingList(
    id = id,
    name = name,
    ownerId = GUEST_OWNER_ID,
    listType = "personal",
    completed = completed,
    starred = starred,
    itemsCount = itemsCount,
    checkedCount = checkedCount,
    participantsCount = 1,
    pendingSync = pendingSync,
    categories = categories,
    items = rootItems,
)

private fun LocalShoppingItemEntity.toModel() = ShoppingItem(
    id = id,
    shoppingListId = listId,
    categoryId = categoryId,
    name = name,
    quantity = quantity,
    checked = checked,
    sortOrder = sortOrder,
    pendingSync = pendingSync,
)

/** Owner fittizio per le liste local-first senza account backend. */
const val GUEST_OWNER_ID = -1L
