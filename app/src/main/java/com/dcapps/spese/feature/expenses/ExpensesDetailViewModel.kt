package com.dcapps.spese.feature.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.data.CatalogRepository
import com.dcapps.spese.core.data.ExpensesRepository
import com.dcapps.spese.core.realtime.RealtimeService
import com.dcapps.spese.core.realtime.RealtimeTable
import com.dcapps.spese.core.model.BalanceEntry
import com.dcapps.spese.core.model.BalanceTotal
import com.dcapps.spese.core.model.CreateExpenseRequest
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.model.ExpenseType
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.core.model.ExpensesListParticipant
import com.dcapps.spese.core.model.UpdateExpenseRequest
import com.dcapps.spese.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class ExpenseSort { DATE, NAME, AMOUNT }

data class ExpensesDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val list: ExpensesList? = null,
    val expenses: List<Expense> = emptyList(),
    val participants: List<ExpensesListParticipant> = emptyList(),
    val expenseTypes: List<ExpenseType> = emptyList(),
    val balanceTotals: List<BalanceTotal> = emptyList(),
    val balanceEntries: List<BalanceEntry> = emptyList(),
    val query: String = "",
    val sort: ExpenseSort = ExpenseSort.DATE,
    val sortAsc: Boolean = false,
    val message: String? = null,
) {
    val total: BigDecimal
        get() = expenses.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }

    val visibleExpenses: List<Expense>
        get() {
            val filtered = expenses.filter {
                query.isBlank() || it.name.contains(query, ignoreCase = true)
            }
            val cmp: Comparator<Expense> = when (sort) {
                ExpenseSort.DATE -> compareBy { it.expenseDate ?: it.createdAt }
                ExpenseSort.NAME -> compareBy { it.name.lowercase() }
                ExpenseSort.AMOUNT -> compareBy { it.amount }
            }
            return filtered.sortedWith(if (sortAsc) cmp else cmp.reversed())
        }
}

@HiltViewModel
class ExpensesDetailViewModel @Inject constructor(
    private val expensesRepository: ExpensesRepository,
    private val catalogRepository: CatalogRepository,
    private val realtimeService: RealtimeService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val listId: Long = checkNotNull(savedStateHandle["listId"])

    private val _uiState = MutableStateFlow(ExpensesDetailUiState())
    val uiState: StateFlow<ExpensesDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        observeRealtime()
    }

    private fun observeRealtime() {
        // Spese della lista: ricarica spese + saldo ad ogni cambiamento.
        viewModelScope.launch {
            realtimeService.watch(RealtimeTable.EXPENSES).collect { reloadExpenses() }
        }
        // Partecipanti: ricarica l'intero dettaglio (cambia la composizione della lista).
        viewModelScope.launch {
            realtimeService.watch(RealtimeTable.EXPENSES_LIST_PARTICIPANTS).collect { load() }
        }
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val listDeferred = async { expensesRepository.getList(listId) }
            val expensesDeferred = async { expensesRepository.getExpenses(listId) }
            val typesDeferred = async { catalogRepository.getExpenseTypes() }
            val balanceDeferred = async { expensesRepository.getBalance(listId) }

            val listResult = listDeferred.await()
            val expensesResult = expensesDeferred.await()
            val typesResult = typesDeferred.await()
            val balanceResult = balanceDeferred.await()

            if (listResult is ApiResult.Error) {
                _uiState.update { it.copy(isLoading = false, error = listResult.message) }
                return@launch
            }
            val list = (listResult as ApiResult.Success).data
            val expenses = (expensesResult as? ApiResult.Success)?.data ?: emptyList()
            val types = (typesResult as? ApiResult.Success)?.data ?: emptyList()
            val balance = (balanceResult as? ApiResult.Success)?.data

            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = null,
                    list = list,
                    participants = list.participants,
                    expenses = expenses,
                    expenseTypes = types,
                    balanceTotals = balance?.totals ?: emptyList(),
                    balanceEntries = balance?.balance ?: emptyList(),
                )
            }
        }
    }

    private fun reloadExpenses() {
        viewModelScope.launch {
            when (val result = expensesRepository.getExpenses(listId)) {
                is ApiResult.Success -> _uiState.update { it.copy(expenses = result.data) }
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
            // Il saldo dipende dalle spese: ricaricalo.
            (expensesRepository.getBalance(listId) as? ApiResult.Success)?.let { bal ->
                _uiState.update {
                    it.copy(balanceTotals = bal.data.totals, balanceEntries = bal.data.balance)
                }
            }
        }
    }

    fun onQueryChange(q: String) = _uiState.update { it.copy(query = q) }

    fun onSortChange(sort: ExpenseSort) = _uiState.update {
        if (it.sort == sort) it.copy(sortAsc = !it.sortAsc) else it.copy(sort = sort, sortAsc = false)
    }

    fun addExpense(
        name: String,
        amount: BigDecimal,
        ownerUserId: Long,
        date: Instant,
        expenseTypeId: Long?,
    ) {
        viewModelScope.launch {
            val result = expensesRepository.createExpense(
                CreateExpenseRequest(
                    name = name.trim(),
                    amount = amount,
                    expenseOwnerUserId = ownerUserId,
                    expenseListId = listId,
                    expenseDate = date.toApiDate(),
                    expenseTypeId = expenseTypeId,
                )
            )
            when (result) {
                is ApiResult.Success -> reloadExpenses()
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun updateExpense(
        id: Long,
        name: String,
        amount: BigDecimal,
        ownerUserId: Long,
        date: Instant,
        expenseTypeId: Long?,
        modifiedBy: Long?,
    ) {
        viewModelScope.launch {
            val result = expensesRepository.updateExpense(
                id,
                UpdateExpenseRequest(
                    name = name.trim(),
                    amount = amount,
                    expenseDate = date.toApiDate(),
                    expenseOwnerUserId = ownerUserId,
                    expenseTypeId = expenseTypeId,
                    modifiedBy = modifiedBy,
                ),
            )
            when (result) {
                is ApiResult.Success -> reloadExpenses()
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            when (val result = expensesRepository.deleteExpense(expense.id)) {
                is ApiResult.Success -> reloadExpenses()
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun Instant.toApiDate(): String =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(this)
}
