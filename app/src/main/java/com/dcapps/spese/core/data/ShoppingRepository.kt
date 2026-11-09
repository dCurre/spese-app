package com.dcapps.spese.core.data

import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.local.ChecklistLocalDataSource
import com.dcapps.spese.core.sync.ChecklistSyncEngine
import com.dcapps.spese.core.model.BatchSaveRequest
import com.dcapps.spese.core.model.CreateShoppingListRequest
import com.dcapps.spese.core.model.JoinRequest
import com.dcapps.spese.core.model.ShoppingList
import com.dcapps.spese.core.model.UpdateShoppingListRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.SpeseApi
import com.dcapps.spese.core.network.apiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checklist LOCAL-FIRST: i DATI delle liste vivono sempre su Room (per tutti), la UI è istantanea e
 * il SyncEngine allinea col backend in background. Le operazioni di CONDIVISIONE (inviti/join) sono
 * intrinsecamente online: vanno dritte al backend se loggato, bloccate in guest.
 *
 * Espone sia letture reattive ([observeLists]/[observeList], Flow) sia operazioni one-shot
 * ([getByUser]/[getList], compatibili con i ViewModel attuali in attesa del refactor reattivo).
 */
@Singleton
class ShoppingRepository @Inject constructor(
    private val api: SpeseApi,
    private val json: Json,
    private val session: SessionManager,
    private val local: ChecklistLocalDataSource,
    private val syncEngine: ChecklistSyncEngine,
) {
    // --- Letture reattive (local-first) ---
    fun observeLists(): Flow<List<ShoppingList>> = local.observeLists()
    fun observeList(id: Long): Flow<ShoppingList?> = local.observeList(id)

    // --- Letture one-shot (compat VM attuali: prima emissione del Flow locale) ---
    suspend fun getByUser(userId: Long): ApiResult<List<ShoppingList>> =
        ApiResult.Success(local.observeLists().first())

    suspend fun getList(id: Long): ApiResult<ShoppingList> {
        val list = local.observeList(id).first()
            ?: return ApiResult.Error(message = "Lista non trovata")
        return ApiResult.Success(list)
    }

    // --- Scritture (sempre Room; dopo ogni scrittura si richiede un sync debounced) ---
    suspend fun create(body: CreateShoppingListRequest): ApiResult<Long?> =
        afterWrite(local.createList(body.name).let {
            when (it) {
                is ApiResult.Success -> ApiResult.Success(it.data)
                is ApiResult.Error -> it
            }
        })

    suspend fun update(id: Long, body: UpdateShoppingListRequest): ApiResult<Unit> =
        afterWrite(local.updateList(id, body.name, body.completed, body.starred))

    suspend fun delete(id: Long): ApiResult<Unit> = afterWrite(local.deleteList(id))

    suspend fun deleteBulk(ids: List<Long>): ApiResult<Unit> = afterWrite(local.deleteLists(ids))

    suspend fun batchSave(id: Long, body: BatchSaveRequest): ApiResult<Unit> = afterWrite(local.applyBatch(id, body))

    /** Dopo una scrittura locale riuscita, richiede un sync (debounced). Passa oltre l'esito. */
    private fun <T> afterWrite(result: ApiResult<T>): ApiResult<T> {
        if (result is ApiResult.Success) syncEngine.requestSync()
        return result
    }

    // --- Condivisione: online diretto se loggato, bloccata in guest ---
    suspend fun createInviteToken(id: Long): ApiResult<String> =
        if (session.isGuest) guestLocked()
        else apiCall(json) { api.createInviteToken(id).inviteToken }

    suspend fun joinByToken(token: String, userId: Long): ApiResult<Long?> =
        if (session.isGuest) guestLocked()
        else apiCall(json) { api.joinShoppingList(token, JoinRequest(userId)).id }

    private fun <T> guestLocked(): ApiResult<T> =
        ApiResult.Error(message = "Accedi con un account per condividere le liste.")
}
