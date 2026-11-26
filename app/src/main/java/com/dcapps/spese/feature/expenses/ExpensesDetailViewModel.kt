package com.dcapps.spese.feature.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.data.CatalogRepository
import com.dcapps.spese.core.data.ExpensesRepository
import com.dcapps.spese.core.model.BalanceEntry
import com.dcapps.spese.core.model.BalanceTotal
import com.dcapps.spese.core.model.CreateExpenseRequest
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.model.ExpenseType
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.core.model.ExpensesListParticipant
import com.dcapps.spese.core.model.UpdateExpenseRequest
import com.dcapps.spese.core.model.UpdateExpensesListRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.prefs.SortCriterion
import com.dcapps.spese.core.prefs.SortPreferences
import com.dcapps.spese.core.prefs.UiPreferences
import com.dcapps.spese.core.prefs.comparator
import com.dcapps.spese.core.prefs.toSaved
import com.dcapps.spese.core.prefs.toSortCriteria
import com.dcapps.spese.core.prefs.toggled
import com.dcapps.spese.core.ui.SelectionController
import com.dcapps.spese.core.ui.launchMutation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    // Ordinamento multi-campo a cascata (ordine = priorità). Vuoto = default (data, più recenti prima).
    val sortCriteria: List<SortCriterion<ExpenseSort>> = emptyList(),
    val shareLink: String? = null,
    val message: String? = null,
    // Nome passato via nav-arg per mostrare subito il titolo corretto, prima che i dati arrivino.
    val placeholderName: String? = null,
    // Mostra il badge "in attesa di sync" sulle righe (preferenza utente).
    val showPendingSync: Boolean = true,
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
            val cmp = sortCriteria.comparator<Expense, ExpenseSort> { key ->
                when (key) {
                    ExpenseSort.DATE -> { e -> e.expenseDate ?: e.createdAt }
                    ExpenseSort.NAME -> { e -> e.name.lowercase() }
                    ExpenseSort.AMOUNT -> { e -> e.amount }
                }
            } ?: compareByDescending { it.expenseDate ?: it.createdAt } // default: più recenti prima
            return filtered.sortedWith(cmp)
        }
}

@HiltViewModel
class ExpensesDetailViewModel @Inject constructor(
    private val expensesRepository: ExpensesRepository,
    private val catalogRepository: CatalogRepository,
    private val uiPreferences: UiPreferences,
    private val sortPreferences: SortPreferences,
    private val session: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val listId: Long = checkNotNull(savedStateHandle["listId"])
    private val navName: String? = savedStateHandle["name"]

    /** true se la sessione è ospite: la UI oscura/lucchetta le azioni di condivisione. */
    val isGuest: Boolean get() = session.isGuest

    private val _uiState = MutableStateFlow(
        ExpensesDetailUiState(
            placeholderName = navName,
            // Ordinamento multi-campo ricordato (ora persistito, prima non lo era).
            sortCriteria = sortPreferences.getList(SortPreferences.EXPENSE_ITEMS)
                .toSortCriteria { runCatching { ExpenseSort.valueOf(it) }.getOrNull() },
        ),
    )
    val uiState: StateFlow<ExpensesDetailUiState> = _uiState.asStateFlow()

    init {
        // Local-first: osserva lista + spese da Room. Ogni modifica rinfresca la UI.
        viewModelScope.launch {
            expensesRepository.observeList(listId)
                .combine(expensesRepository.observeExpenses(listId)) { list, expenses -> list to expenses }
                .collect { (list, expenses) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            list = list,
                            participants = list?.participants ?: emptyList(),
                            expenses = expenses,
                        )
                    }
                    // Il saldo dipende dalle spese: ricalcolo (per ora vuoto in local-first).
                    reloadBalance()
                }
        }
        loadExpenseTypes()
        viewModelScope.launch {
            uiPreferences.showPendingSyncBadges.collect { show ->
                _uiState.update { it.copy(showPendingSync = show) }
            }
        }
    }

    /** Compat: con l'osservazione reattiva il reload manuale non serve più (no-op). */
    fun load(showLoading: Boolean = false) { /* reattivo: niente da fare */ }

    private fun loadExpenseTypes() {
        viewModelScope.launch {
            (catalogRepository.getExpenseTypes() as? ApiResult.Success)?.let { res ->
                _uiState.update { it.copy(expenseTypes = res.data) }
            }
        }
    }

    private fun reloadBalance() {
        viewModelScope.launch {
            (expensesRepository.getBalance(listId) as? ApiResult.Success)?.let { bal ->
                _uiState.update {
                    it.copy(balanceTotals = bal.data.totals, balanceEntries = bal.data.balance)
                }
            }
        }
    }

    fun onQueryChange(q: String) = _uiState.update { it.copy(query = q) }

    fun onSortToggle(sort: ExpenseSort) {
        _uiState.update { it.copy(sortCriteria = it.sortCriteria.toggled(sort)) }
        sortPreferences.setList(SortPreferences.EXPENSE_ITEMS, _uiState.value.sortCriteria.toSaved())
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
            onSuccess = { },
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
            onSuccess = { },
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
            onSuccess = { },
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
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    // --- Gestione lista (condivisione, partecipanti, ospiti, proprietà) — ONLINE ---

    /** Recupera (o genera) il token e prepara il link di invito della lista. */
    fun share() {
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { token ->
                _uiState.update { it.copy(shareLink = "https://spese-dc.web.app/join?token=$token") }
            },
            call = { expensesRepository.createInviteToken(listId) },
        )
    }

    fun clearShareLink() = _uiState.update { it.copy(shareLink = null) }

    fun addGuest(name: String) {
        if (name.isBlank()) return
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { },
            call = { expensesRepository.addGuest(listId, name) },
        )
    }

    fun removeParticipant(p: ExpensesListParticipant) {
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { },
            call = {
                if (p.isGuest) expensesRepository.removeGuest(listId, p.userId)
                else expensesRepository.removeParticipant(listId, p.userId)
            },
        )
    }

    /** Rinomina la lista (dato local-first: scrive su Room, il Flow aggiorna la UI). */
    fun renameList(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val r = expensesRepository.updateList(listId, UpdateExpensesListRequest(name = newName.trim()))
            if (r is ApiResult.Error) _uiState.update { it.copy(message = r.message) }
        }
    }

    val currentUserId: Long? get() = session.currentUser.value?.id

    /** true se l'utente loggato è il proprietario (può cambiare il tipo della lista). (#11) */
    val isOwner: Boolean
        get() = _uiState.value.list?.userId != null &&
            _uiState.value.list?.userId == session.currentUser.value?.id

    /**
     * Cambia il tipo della lista spese (condivisa ↔ personale). Operazione ONLINE: richiede
     * requester_id (owner), bloccata in guest. (issue #11)
     */
    fun setListType(shared: Boolean) {
        val requesterId = session.currentUser.value?.id ?: return
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { },
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
            onSuccess = { },
            call = { expensesRepository.transferOwner(listId, newOwnerId, currentOwner) },
        )
    }

    private fun Instant.toApiDate(): String =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(this)
}
