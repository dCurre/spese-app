package com.dcapps.spese.core.data

import com.dcapps.spese.core.local.ChecklistLocalDataSource
import com.dcapps.spese.core.sync.ChecklistSyncEngine
import com.dcapps.spese.core.model.CreateShoppingCategoryRequest
import com.dcapps.spese.core.model.CreateShoppingItemRequest
import com.dcapps.spese.core.model.UpdateShoppingCategoryRequest
import com.dcapps.spese.core.model.UpdateShoppingItemRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.mapTo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Articoli e categorie delle checklist, LOCAL-FIRST: tutto su Room (per tutti). Il SyncEngine
 * allinea col backend in background. Stesse firme di prima, stesso [ApiResult], così i ViewModel
 * non cambiano.
 */
@Singleton
class ShoppingItemRepository @Inject constructor(
    private val local: ChecklistLocalDataSource,
    private val syncEngine: ChecklistSyncEngine,
) {
    suspend fun create(body: CreateShoppingItemRequest): ApiResult<Long?> =
        afterWrite(local.addItem(body.shoppingListId, body.name, body.categoryId).mapTo<Long, Long?>(null))

    suspend fun update(id: Long, body: UpdateShoppingItemRequest): ApiResult<Unit> =
        afterWrite(local.updateItem(id, body.name ?: "", body.quantity ?: 1, body.categoryId))

    /** Aggiorna nome, quantità e categoria (category_id esplicito, anche null = nessuna categoria). */
    suspend fun updateWithCategory(
        id: Long,
        name: String,
        quantity: Int,
        categoryId: Long?,
    ): ApiResult<Unit> = afterWrite(local.updateItem(id, name, quantity, categoryId))

    suspend fun delete(id: Long): ApiResult<Unit> = afterWrite(local.deleteItem(id))

    suspend fun deleteBulk(ids: List<Long>): ApiResult<Unit> {
        ids.forEach { local.deleteItem(it) }
        return afterWrite(ApiResult.Success(Unit))
    }

    suspend fun check(id: Long, checked: Boolean?): ApiResult<Boolean> =
        afterWrite(local.setItemChecked(id, checked == true).mapTo(checked == true))

    suspend fun checkAll(listId: Long, checked: Boolean): ApiResult<Unit> =
        afterWrite(local.setAllChecked(listId, checked))

    suspend fun reorder(listId: Long, orderedIds: List<Long>): ApiResult<Unit> =
        ApiResult.Success(Unit) // riordino locale non ancora gestito; no-op

    suspend fun createCategory(body: CreateShoppingCategoryRequest): ApiResult<Long?> =
        afterWrite(local.addCategory(body.shoppingListId, body.name, body.parentId).mapTo<Long, Long?>(null))

    suspend fun updateCategory(id: Long, body: UpdateShoppingCategoryRequest): ApiResult<Unit> =
        afterWrite(local.updateCategory(id, listId = local.listIdOfCategory(id), name = body.name ?: "", parentId = null))

    /** Aggiorna nome e categoria padre (parent_id esplicito, anche null = radice). */
    suspend fun updateCategoryWithParent(
        id: Long,
        name: String,
        parentId: Long?,
    ): ApiResult<Unit> =
        afterWrite(local.updateCategory(id, listId = local.listIdOfCategory(id), name = name, parentId = parentId))

    suspend fun deleteCategory(id: Long): ApiResult<Unit> = afterWrite(local.deleteCategory(id))

    /** Dopo una scrittura locale riuscita, richiede un sync (debounced). Passa oltre l'esito. */
    private fun <T> afterWrite(result: ApiResult<T>): ApiResult<T> {
        if (result is ApiResult.Success) syncEngine.requestSync()
        return result
    }
}
