package com.dcapps.spese.feature.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.data.CatalogRepository
import com.dcapps.spese.core.data.ExpensesRepository
import com.dcapps.spese.core.realtime.RealtimeService
import com.dcapps.spese.core.realtime.RealtimeTable
import com.dcapps.spese.core.realtime.longField
import com.dcapps.spese.core.model.BalanceEntry
import com.dcapps.spese.core.model.BalanceTotal
import com.dcapps.spese.core.model.CreateExpenseRequest
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.model.ExpenseType
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.core.model.ExpensesListParticipant
import com.dcapps.spese.core.model.UpdateExpenseRequest
import com.dcapps.spese.core.model.UpdateExpensesListRequest
import com.dcapps.spese.core.model.User
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.ui.SelectionController
import com.dcapps.spese.core.ui.launchMutation
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
    val availableUsers: List<User> = emptyList(),
    val shareLink: String? = null,
    val message: String? = null,
    // Nome passato via nav-arg per mostrare subito il titolo corretto, prima che getList risponda.
    val placeholderName: String? = null,
) {
    /** Nome da mostrare: quello reale se caricato, altrimenti il placeholder dal nav-arg. */
    val displayName: String? get() = list?.name ?: placeholderName?.takeIf { it.isNotBlank() }

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
    private val session: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val listId: Long = checkNotNull(savedStateHandle["listId"])
    private val navName: String? = savedStateHandle["name"]

    private val _uiState = MutableStateFlow(ExpensesDetailUiState(placeholderName = navName))
    val uiState: StateFlow<ExpensesDetailUiState> = _uiState.asStateFlow()

    init {
        load(showLoading = true)
        observeRealtime()
    }

    private fun observeRealtime() {
        // Spese della lista: ricarica solo se l'evento riguarda questa lista.
        viewModelScope.launch {
            realtimeService.watch(RealtimeTable.EXPENSES).collect { action ->
                if (action.longField("expense_list_id") == listId) reloadExpenses()
            }
        }
        // Partecipanti: ricarica il dettaglio solo per questa lista.
        viewModelScope.launch {
            realtimeService.watch(RealtimeTable.EXPENSES_LIST_PARTICIPANTS).collect { action ->
                if (action.longField("expenses_list_id") == listId) load()
            }
        }
    }

    /**
     * Ricarica il dettaglio lista. [showLoading] = true mostra lo spinner a tutta pagina (solo
     * primo caricamento); i reload da realtime/mutazioni strutturali usano false (niente lampeggio).
     */
    fun load(showLoading: Boolean = false) {
        if (showLoading) _uiState.update { it.copy(isLoading = true, error = null) }
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
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { reloadExpenses() },
            call = {
                expensesRepository.createExpense(
                    CreateExpenseRequest(
                        name = name.trim(),
                        amount = amount,
                        expenseOwnerUserId = ownerUserId,
                        expenseListId = listId,
                        expenseDate = date.toApiDate(),
                        expenseTypeId = expenseTypeId,
                    )
                )
            },
        )
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
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { reloadExpenses() },
            call = {
                expensesRepository.updateExpense(
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
            },
        )
    }

    fun deleteExpense(expense: Expense) {
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { reloadExpenses() },
            call = { expensesRepository.deleteExpense(expense.id) },
        )
    }

    // --- Selezione multipla (issue #10) ---
    val selection = SelectionController()

    /** Elimina tutte le spese selezionate in UNA chiamata bulk, poi esce dalla selezione. */
    fun deleteSelected() {
        val ids = selection.ids.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val r = expensesRepository.deleteExpensesBulk(ids)
            selection.clear()
            if (r is ApiResult.Error) _uiState.update { it.copy(message = r.message) }
            reloadExpenses()
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    // --- Gestione lista (condivisione, partecipanti, ospiti, proprietà) ---

    fun share() {
        _uiState.update { it.copy(shareLink = "https://spese-dc.web.app/join?list=$listId") }
    }

    fun clearShareLink() = _uiState.update { it.copy(shareLink = null) }

    fun loadAvailableUsers() {
        viewModelScope.launch {
            val already = _uiState.value.participants.map { it.userId }.toSet()
            (expensesRepository.getAllUsers() as? ApiResult.Success)?.let { res ->
                _uiState.update { s -> s.copy(availableUsers = res.data.filter { it.id !in already }) }
            }
        }
    }

    fun addParticipant(userId: Long) {
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { load() },
            call = { expensesRepository.addParticipant(listId, userId) },
        )
    }

    fun addGuest(name: String) {
        if (name.isBlank()) return
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { load() },
            call = { expensesRepository.addGuest(listId, name) },
        )
    }

    fun removeParticipant(p: ExpensesListParticipant) {
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { load() },
            call = {
                if (p.isGuest) expensesRepository.removeGuest(listId, p.userId)
                else expensesRepository.removeParticipant(listId, p.userId)
            },
        )
    }

    /** Rinomina la lista (unica proprietà modificabile lato backend oltre a `paid`). */
    fun renameList(newName: String) {
        if (newName.isBlank()) return
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { load() },
            call = { expensesRepository.updateList(listId, UpdateExpensesListRequest(name = newName.trim())) },
        )
    }

    /** true se l'utente loggato è il proprietario (può cambiare il tipo della lista). (#11) */
    val isOwner: Boolean
        get() = _uiState.value.list?.userId != null &&
            _uiState.value.list?.userId == session.currentUser.value?.id

    /**
     * Cambia il tipo della lista spese (condivisa ↔ personale). Richiede requester_id (owner).
     * Il passaggio a personale è DISTRUTTIVO: le spese passano all'owner e i partecipanti vengono
     * rimossi. La conferma è a carico della UI. (issue #11)
     */
    fun setListType(shared: Boolean) {
        val requesterId = session.currentUser.value?.id ?: return
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { load() },
            call = {
                expensesRepository.updateList(
                    listId,
                    UpdateExpensesListRequest(
                        listType = if (shared) "shared" else "personal",
                        requesterId = requesterId,
                    ),
                )
            },
        )
    }

    fun transferOwner(newOwnerId: Long) {
        val currentOwner = _uiState.value.list?.userId ?: return
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { load() },
            call = { expensesRepository.transferOwner(listId, newOwnerId, currentOwner) },
        )
    }

    private fun Instant.toApiDate(): String =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(this)
}
