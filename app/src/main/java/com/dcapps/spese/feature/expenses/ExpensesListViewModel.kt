package com.dcapps.spese.feature.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.data.ExpensesRepository
import com.dcapps.spese.core.data.UserRepository
import com.dcapps.spese.core.model.CreateExpensesListRequest
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.core.model.UpdateExpensesListRequest
import com.dcapps.spese.core.model.UpdateUserRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.ConnectivityObserver
import com.dcapps.spese.core.prefs.SortPreferences
import com.dcapps.spese.core.prefs.UiPreferences
import com.dcapps.spese.core.ui.SelectionController
import com.dcapps.spese.core.ui.launchMutation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ListSort { DATE, NAME }

data class ExpensesListUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val activeLists: List<ExpensesList> = emptyList(),
    val paidLists: List<ExpensesList> = emptyList(),
    val query: String = "",
    val sort: ListSort = ListSort.DATE,
    val sortAsc: Boolean = false,
    val paidListsShown: Boolean = true,
    val message: String? = null,
    // Mostra il badge "in attesa di sync" sulle righe (preferenza utente).
    val showPendingSync: Boolean = true,
    // Connesso a Internet: offline la creazione di liste condivise è lucchettata (punto 4).
    val isOnline: Boolean = true,
)

@HiltViewModel
class ExpensesListViewModel @Inject constructor(
    private val expensesRepository: ExpensesRepository,
    private val userRepository: UserRepository,
    private val session: SessionManager,
    private val sortPreferences: SortPreferences,
    private val uiPreferences: UiPreferences,
    private val connectivity: ConnectivityObserver,
) : ViewModel() {

    // Ordinamento ricordato dall'ultima volta (issue #9).
    private val savedSort = sortPreferences.get(SortPreferences.EXPENSES)
    private val _uiState = MutableStateFlow(
        ExpensesListUiState(
            sort = savedSort?.criterion?.let { runCatching { ListSort.valueOf(it) }.getOrNull() }
                ?: ListSort.DATE,
            sortAsc = savedSort?.ascending ?: false,
            paidListsShown = session.currentUser.value?.paidListShown ?: true,
        ),
    )
    val uiState: StateFlow<ExpensesListUiState> = _uiState.asStateFlow()

    /** true se la sessione è ospite: in creazione, l'opzione "condivisa" è lucchettata. */
    val isGuest: Boolean get() = session.isGuest

    private var allLists: List<ExpensesList> = emptyList()

    init {
        // Local-first: osserva Room. Ogni modifica (locale o portata dal sync) rinfresca la UI.
        viewModelScope.launch {
            expensesRepository.observeLists().collect { lists ->
                allLists = lists
                _uiState.update { it.copy(isLoading = false, error = null) }
                recompute()
            }
        }
        viewModelScope.launch {
            uiPreferences.showPendingSyncBadges.collect { show ->
                _uiState.update { it.copy(showPendingSync = show) }
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    /** Compat: con l'osservazione reattiva il refresh manuale non serve più (no-op). */
    fun load(showLoading: Boolean = false) { /* reattivo: niente da fare */ }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        recompute()
    }

    fun onSortChange(sort: ListSort) {
        _uiState.update {
            if (it.sort == sort) it.copy(sortAsc = !it.sortAsc) else it.copy(sort = sort, sortAsc = false)
        }
        // Ricorda la scelta per la prossima apertura (issue #9).
        _uiState.value.let { sortPreferences.set(SortPreferences.EXPENSES, it.sort.name, it.sortAsc) }
        recompute()
    }

    fun togglePaidListsShown() {
        val newValue = !_uiState.value.paidListsShown
        _uiState.update { it.copy(paidListsShown = newValue) }
        // Preferenza UI: se loggato la si persiste sull'utente lato backend; in guest resta locale.
        val user = session.currentUser.value ?: return
        viewModelScope.launch {
            userRepository.update(user.id, UpdateUserRequest(paidListShown = newValue))
            session.setUser(user.copy(paidListShown = newValue))
        }
    }

    fun createList(name: String, shared: Boolean) {
        if (name.isBlank()) return
        val userId = session.currentUser.value?.id ?: 0L
        // Difesa: offline la condivisione non è disponibile → forza personale (punto 4).
        val effectiveShared = shared && connectivity.isOnline.value
        val listType = if (effectiveShared) "shared" else "personal"
        // Local-first: la create scrive su Room; il Flow osservato aggiorna la UI da solo.
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { },
            call = {
                expensesRepository.createList(
                    CreateExpensesListRequest(name = name.trim(), userId = userId, listType = listType),
                )
            },
        )
    }

    fun deleteList(list: ExpensesList) {
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { },
            call = { expensesRepository.deleteList(list.id) },
        )
    }

    fun setPaid(list: ExpensesList, paid: Boolean) {
        viewModelScope.launch {
            expensesRepository.updateList(list.id, UpdateExpensesListRequest(paid = paid))
        }
    }

    // --- Selezione multipla (issue #10) ---
    val selection = SelectionController()

    /** Elimina tutte le liste selezionate in UNA chiamata bulk, poi esce dalla selezione. */
    fun deleteSelected() {
        val ids = selection.ids.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val r = expensesRepository.deleteListsBulk(ids)
            selection.clear()
            if (r is ApiResult.Error) _uiState.update { it.copy(message = r.message) }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun recompute() {
        val state = _uiState.value
        val filtered = allLists.filter {
            state.query.isBlank() || it.name.contains(state.query, ignoreCase = true)
        }
        val base: Comparator<ExpensesList> = when (state.sort) {
            ListSort.DATE -> compareBy { it.createdAt }
            ListSort.NAME -> compareBy { it.name.lowercase() }
        }
        val sorted = filtered.sortedWith(if (state.sortAsc) base else base.reversed())
        _uiState.update {
            it.copy(
                activeLists = sorted.filter { l -> !l.paid },
                paidLists = sorted.filter { l -> l.paid },
            )
        }
    }
}
