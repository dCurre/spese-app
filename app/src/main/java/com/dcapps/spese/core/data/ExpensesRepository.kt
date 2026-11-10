package com.dcapps.spese.core.data

import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.local.ExpensesLocalDataSource
import com.dcapps.spese.core.model.BalanceResponse
import com.dcapps.spese.core.model.CreateExpenseRequest
import com.dcapps.spese.core.model.CreateExpensesListRequest
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.model.ExpenseStats
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.core.model.AddParticipantRequest
import com.dcapps.spese.core.model.CreateUserRequest
import com.dcapps.spese.core.model.TransferOwnerRequest
import com.dcapps.spese.core.model.UpdateExpenseRequest
import com.dcapps.spese.core.model.UpdateExpensesListRequest
import com.dcapps.spese.core.model.User
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.SpeseApi
import com.dcapps.spese.core.network.apiCall
import com.dcapps.spese.core.network.mapTo
import com.dcapps.spese.core.sync.ExpensesSyncEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Spese LOCAL-FIRST: i DATI (liste spese + spese) vivono sempre su Room, la UI è istantanea e
 * reattiva. Dopo ogni scrittura locale riuscita si richiede un sync col backend (delta) via
 * [ExpensesSyncEngine] — vedi [afterWrite]. In guest il sync è no-op (resta tutto locale).
 *
 * Le operazioni di CONDIVISIONE (partecipanti, ospiti, transfer-owner, cambio tipo lista) sono
 * intrinsecamente online: vanno dritte al backend se loggato, bloccate in guest.
 *
 * Espone sia letture reattive ([observeLists]/[observeList]/[observeExpenses], Flow) sia operazioni
 * one-shot compatibili coi ViewModel attuali.
 */
@Singleton
class ExpensesRepository @Inject constructor(
    private val api: SpeseApi,
    private val json: Json,
    private val session: SessionManager,
    private val local: ExpensesLocalDataSource,
    private val syncEngine: ExpensesSyncEngine,
) {
    // --- Letture reattive (local-first) ---
    fun observeLists(): Flow<List<ExpensesList>> = local.observeLists()
    fun observeList(id: Long): Flow<ExpensesList?> = local.observeList(id)
    fun observeExpenses(listId: Long): Flow<List<Expense>> = local.observeExpenses(listId)

    // --- Letture one-shot (compat VM: prima emissione del Flow locale) ---
    suspend fun getLists(paid: Boolean? = null): ApiResult<List<ExpensesList>> =
        ApiResult.Success(local.observeLists().first())

    suspend fun getList(id: Long): ApiResult<ExpensesList> {
        val list = local.observeList(id).first()
            ?: return ApiResult.Error(message = "Lista non trovata")
        return ApiResult.Success(list)
    }

    suspend fun getExpenses(listId: Long): ApiResult<List<Expense>> =
        ApiResult.Success(local.observeExpenses(listId).first())

    // --- Scritture DATI (sempre Room) ---
    suspend fun createList(body: CreateExpensesListRequest): ApiResult<Long?> =
        afterWrite(local.createList(body.name).mapTo<Long, Long?>(null))

    suspend fun updateList(id: Long, body: UpdateExpensesListRequest): ApiResult<Unit> {
        // Cambio tipo lista (#11): operazione online, non è un dato locale → delega al backend.
        if (body.listType != null) return setListTypeOnline(id, body)
        return afterWrite(local.updateList(id, body.name, body.paid))
    }

    suspend fun deleteList(id: Long): ApiResult<Unit> = afterWrite(local.deleteList(id))

    suspend fun deleteListsBulk(ids: List<Long>): ApiResult<Unit> = afterWrite(local.deleteLists(ids))

    suspend fun createExpense(body: CreateExpenseRequest): ApiResult<Long?> =
        afterWrite(
            local.addExpense(
                listId = body.expenseListId,
                name = body.name,
                amount = body.amount,
                expenseDate = body.expenseDate,
                expenseTypeId = body.expenseTypeId,
            ).mapTo<Long, Long?>(null),
        )

    suspend fun updateExpense(id: Long, body: UpdateExpenseRequest): ApiResult<Unit> =
        afterWrite(
            local.updateExpense(
                id = id,
                name = body.name,
                amount = body.amount,
                expenseDate = body.expenseDate,
                expenseTypeId = body.expenseTypeId,
            ),
        )

    suspend fun deleteExpense(id: Long): ApiResult<Unit> = afterWrite(local.deleteExpense(id))

    suspend fun deleteExpensesBulk(ids: List<Long>): ApiResult<Unit> = afterWrite(local.deleteExpenses(ids))

    /**
     * Saldo "chi deve a chi": è una funzione delle sole spese locali. In local-first le liste sono
     * personali (un solo partecipante): nessun saldo da ripartire → risposta vuota. (Il saldo reale
     * delle liste condivise lo calcola il backend quando il sync spese sarà disponibile.)
     */
    suspend fun getBalance(listId: Long): ApiResult<BalanceResponse> =
        ApiResult.Success(BalanceResponse())

    /**
     * Statistiche aggregate delle spese personali dell'utente (dashboard). Online-only: aggregazione
     * lato backend (`GET /api/expenses/stats`). In guest/senza utente ritorna un errore gentile.
     * [from]/[to] sono date yyyy-MM-dd; [granularity] = month|week|year.
     */
    suspend fun getExpenseStats(
        from: String? = null,
        to: String? = null,
        granularity: String? = null,
    ): ApiResult<ExpenseStats> {
        val userId = session.currentUser.value?.id
            ?: return ApiResult.Error(message = "Accedi con un account per vedere le statistiche.")
        return apiCall(json) { api.getExpenseStats(userId, from, to, granularity) }
    }

    /**
     * Dopo una scrittura locale riuscita richiede un sync (debounced) col backend. No-op in guest
     * (il SyncEngine ignora la richiesta finché non c'è un utente loggato).
     */
    private fun <T> afterWrite(result: ApiResult<T>): ApiResult<T> {
        if (result is ApiResult.Success) syncEngine.requestSync()
        return result
    }

    // --- Condivisione: online diretto se loggato, bloccata in guest ---

    suspend fun addParticipant(listId: Long, userId: Long): ApiResult<Unit> =
        if (session.isGuest) guestLocked()
        else apiCall(json) { api.addParticipant(listId, AddParticipantRequest(userId)); Unit }

    suspend fun removeParticipant(listId: Long, userId: Long): ApiResult<Unit> =
        if (session.isGuest) guestLocked()
        else apiCall(json) { api.removeParticipant(listId, userId); Unit }

    suspend fun removeGuest(listId: Long, userId: Long): ApiResult<Unit> =
        if (session.isGuest) guestLocked()
        else apiCall(json) { api.removeGuestParticipant(listId, userId); Unit }

    /** Crea un ospite (utente senza account) e lo aggiunge come partecipante alla lista. */
    suspend fun addGuest(listId: Long, name: String): ApiResult<Unit> =
        if (session.isGuest) guestLocked()
        else apiCall(json) {
            val created = api.createUser(CreateUserRequest(name = name.trim(), isGuest = true))
            val guestId = created.id ?: error("ID ospite mancante")
            api.addParticipant(listId, AddParticipantRequest(guestId))
            Unit
        }

    suspend fun transferOwner(listId: Long, newOwnerId: Long, currentOwnerId: Long): ApiResult<Unit> =
        if (session.isGuest) guestLocked()
        else apiCall(json) {
            api.transferExpensesListOwner(
                listId,
                TransferOwnerRequest(newOwnerId = newOwnerId, currentOwnerId = currentOwnerId),
            ); Unit
        }

    /** Tutti gli utenti registrati (per aggiungere un partecipante esistente). Online. */
    suspend fun getAllUsers(): ApiResult<List<User>> =
        if (session.isGuest) guestLocked()
        else apiCall(json) { api.getUsers().users.filter { !it.isGuest } }

    /** Cambio tipo lista (#11): online, richiede requester_id (owner). Bloccato in guest. */
    private suspend fun setListTypeOnline(id: Long, body: UpdateExpensesListRequest): ApiResult<Unit> =
        if (session.isGuest) guestLocked()
        else apiCall(json) { api.updateExpensesList(id, body); Unit }

    // Fallback di sicurezza: in UI il tap sulle azioni di condivisione è già intercettato per il guest
    // (lucchetto → messaggio localizzato R.string.guest_share_locked), quindi questo ramo non dovrebbe
    // essere raggiunto. Resta come rete di protezione lato dati; il messaggio utente usa la risorsa.
    private fun <T> guestLocked(): ApiResult<T> =
        ApiResult.Error(message = "Accedi con un account per condividere le liste.")
}
