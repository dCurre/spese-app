package com.dcapps.spese.core.data

import com.dcapps.spese.core.model.BatchSaveRequest
import com.dcapps.spese.core.model.CheckRequest
import com.dcapps.spese.core.model.CreateShoppingListRequest
import com.dcapps.spese.core.model.JoinRequest
import com.dcapps.spese.core.model.ShoppingList
import com.dcapps.spese.core.model.UpdateShoppingListRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.SpeseApi
import com.dcapps.spese.core.network.apiCall
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Accesso ai dati delle checklist (liste della spesa). */
@Singleton
class ShoppingRepository @Inject constructor(
    private val api: SpeseApi,
    private val json: Json,
) {
    suspend fun getByUser(userId: Long): ApiResult<List<ShoppingList>> =
        apiCall(json) { api.getShoppingListsByUser(userId).shoppingLists }

    suspend fun getList(id: Long): ApiResult<ShoppingList> =
        apiCall(json) { api.getShoppingList(id) }

    suspend fun create(body: CreateShoppingListRequest): ApiResult<Long?> =
        apiCall(json) { api.createShoppingList(body).id }

    suspend fun update(id: Long, body: UpdateShoppingListRequest): ApiResult<Unit> =
        apiCall(json) { api.updateShoppingList(id, body); Unit }

    suspend fun delete(id: Long): ApiResult<Unit> =
        apiCall(json) { api.deleteShoppingList(id); Unit }

    suspend fun batchSave(id: Long, body: BatchSaveRequest): ApiResult<Unit> =
        apiCall(json) { api.batchSave(id, body); Unit }

    suspend fun checkItem(itemId: Long, checked: Boolean?): ApiResult<Boolean> =
        apiCall(json) { api.checkShoppingItem(itemId, CheckRequest(checked)).checked }

    suspend fun checkCategory(categoryId: Long, checked: Boolean): ApiResult<Unit> =
        apiCall(json) { api.checkShoppingCategory(categoryId, CheckRequest(checked)); Unit }

    suspend fun createInviteToken(id: Long): ApiResult<String> =
        apiCall(json) { api.createInviteToken(id).inviteToken }

    suspend fun joinByToken(token: String, userId: Long): ApiResult<Long?> =
        apiCall(json) { api.joinShoppingList(token, JoinRequest(userId)).id }
}
