package com.dcapps.spese.core.data

import com.dcapps.spese.core.model.CheckAllRequest
import com.dcapps.spese.core.model.CheckRequest
import com.dcapps.spese.core.model.CreateShoppingCategoryRequest
import com.dcapps.spese.core.model.CreateShoppingItemRequest
import com.dcapps.spese.core.model.ReorderRequest
import com.dcapps.spese.core.model.UpdateShoppingItemRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.SpeseApi
import com.dcapps.spese.core.network.apiCall
import kotlinx.serialization.json.Json
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

    suspend fun deleteCategory(id: Long): ApiResult<Unit> =
        apiCall(json) { api.deleteShoppingCategory(id); Unit }
}
