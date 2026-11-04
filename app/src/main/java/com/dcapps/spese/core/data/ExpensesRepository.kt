package com.dcapps.spese.core.data

import com.dcapps.spese.core.model.BalanceResponse
import com.dcapps.spese.core.model.CreateExpenseRequest
import com.dcapps.spese.core.model.CreateExpensesListRequest
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.core.model.AddParticipantRequest
import com.dcapps.spese.core.model.UpdateExpenseRequest
import com.dcapps.spese.core.model.UpdateExpensesListRequest
import com.dcapps.spese.core.model.UserWithExpensesListsResponse
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.SpeseApi
import com.dcapps.spese.core.network.apiCall
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Accesso ai dati di liste spese, spese e saldi. */
@Singleton
class ExpensesRepository @Inject constructor(
    private val api: SpeseApi,
    private val json: Json,
) {
    suspend fun getLists(paid: Boolean? = null): ApiResult<List<ExpensesList>> =
        apiCall(json) { api.getExpensesLists(paid).expensesLists }

    /**
     * Carica utente + sue liste in una chiamata (come la home della webapp).
     * L'email va URL-encoded dal chiamante non è necessario: Retrofit gestisce l'encoding del path.
     */
    suspend fun getUserWithLists(email: String): ApiResult<UserWithExpensesListsResponse> =
        apiCall(json) { api.getUserExpensesLists(email) }

    suspend fun getList(id: Long): ApiResult<ExpensesList> =
        apiCall(json) { api.getExpensesList(id) }

    suspend fun createList(body: CreateExpensesListRequest): ApiResult<Long?> =
        apiCall(json) { api.createExpensesList(body).id }

    suspend fun updateList(id: Long, body: UpdateExpensesListRequest): ApiResult<Unit> =
        apiCall(json) { api.updateExpensesList(id, body); Unit }

    suspend fun deleteList(id: Long): ApiResult<Unit> =
        apiCall(json) { api.deleteExpensesList(id); Unit }

    suspend fun addParticipant(listId: Long, userId: Long): ApiResult<Unit> =
        apiCall(json) { api.addParticipant(listId, AddParticipantRequest(userId)); Unit }

    suspend fun removeParticipant(listId: Long, userId: Long): ApiResult<Unit> =
        apiCall(json) { api.removeParticipant(listId, userId); Unit }

    suspend fun getExpenses(listId: Long): ApiResult<List<Expense>> =
        apiCall(json) { api.getExpensesByList(listId).expenses }

    suspend fun createExpense(body: CreateExpenseRequest): ApiResult<Long?> =
        apiCall(json) { api.createExpense(body).id }

    suspend fun updateExpense(id: Long, body: UpdateExpenseRequest): ApiResult<Unit> =
        apiCall(json) { api.updateExpense(id, body); Unit }

    suspend fun deleteExpense(id: Long): ApiResult<Unit> =
        apiCall(json) { api.deleteExpense(id); Unit }

    suspend fun getBalance(listId: Long): ApiResult<BalanceResponse> =
        apiCall(json) { api.getBalance(listId) }
}
