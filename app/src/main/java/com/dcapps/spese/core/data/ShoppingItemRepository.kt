package com.dcapps.spese.core.data

import com.dcapps.spese.core.model.CheckAllRequest
import com.dcapps.spese.core.model.CheckRequest
import com.dcapps.spese.core.model.CreateShoppingCategoryRequest
import com.dcapps.spese.core.model.CreateShoppingItemRequest
import com.dcapps.spese.core.model.ReorderRequest
import com.dcapps.spese.core.model.UpdateShoppingCategoryRequest
import com.dcapps.spese.core.model.UpdateShoppingItemRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.SpeseApi
import com.dcapps.spese.core.network.apiCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

/** Accesso ai dati di articoli e categorie delle checklist. */
@Singleton
class ShoppingItemRepository @Inject constructor(
    private val api: SpeseApi,
    private val json: Json,
) {
    suspend fun create(body: CreateShoppingItemRequest): ApiResult<Long?> =
        apiCall(json) { api.createShoppingItem(body).id }

    suspend fun update(id: Long, body: UpdateShoppingItemRequest): ApiResult<Unit> =
        apiCall(json) { api.updateShoppingItem(id, body); Unit }

    /**
     * Aggiorna nome, quantità e categoria inviando `category_id` SEMPRE esplicito (anche null),
     * così lo spostamento in "nessuna categoria" funziona nonostante explicitNulls=false.
     */
    suspend fun updateWithCategory(
        id: Long,
        name: String,
        quantity: Int,
        categoryId: Long?,
    ): ApiResult<Unit> = apiCall(json) {
        val body = buildJsonObject {
            put("name", JsonPrimitive(name))
            put("quantity", JsonPrimitive(quantity))
            put("category_id", if (categoryId != null) JsonPrimitive(categoryId) else JsonNull)
        }
        api.updateShoppingItemRaw(id, body); Unit
    }

    suspend fun delete(id: Long): ApiResult<Unit> =
        apiCall(json) { api.deleteShoppingItem(id); Unit }

    suspend fun check(id: Long, checked: Boolean?): ApiResult<Boolean> =
        apiCall(json) { api.checkShoppingItem(id, CheckRequest(checked)).checked }

    suspend fun checkAll(listId: Long, checked: Boolean): ApiResult<Unit> =
        apiCall(json) { api.checkAllShoppingItems(CheckAllRequest(listId, checked)); Unit }

    suspend fun reorder(listId: Long, orderedIds: List<Long>): ApiResult<Unit> =
        apiCall(json) { api.reorderShoppingItems(ReorderRequest(listId, orderedIds)); Unit }

    suspend fun createCategory(body: CreateShoppingCategoryRequest): ApiResult<Long?> =
        apiCall(json) { api.createShoppingCategory(body).id }

    suspend fun updateCategory(id: Long, body: UpdateShoppingCategoryRequest): ApiResult<Unit> =
        apiCall(json) { api.updateShoppingCategory(id, body); Unit }

    /** Aggiorna nome e categoria padre inviando `parent_id` SEMPRE esplicito (anche null = radice). */
    suspend fun updateCategoryWithParent(
        id: Long,
        name: String,
        parentId: Long?,
    ): ApiResult<Unit> = apiCall(json) {
        val body = buildJsonObject {
            put("name", JsonPrimitive(name))
            put("parent_id", if (parentId != null) JsonPrimitive(parentId) else JsonNull)
        }
        api.updateShoppingCategoryRaw(id, body); Unit
    }

    suspend fun deleteCategory(id: Long): ApiResult<Unit> =
        apiCall(json) { api.deleteShoppingCategory(id); Unit }
}
