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
import com.dcapps.spese.core.prefs.SortCriterion
import com.dcapps.spese.core.prefs.SortPreferences
import com.dcapps.spese.core.prefs.comparator
import com.dcapps.spese.core.prefs.toSaved
import com.dcapps.spese.core.prefs.toSortCriteria
import com.dcapps.spese.core.prefs.toggled
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
    // Ordinamento multi-campo a cascata (ordine = priorità). Vuoto = default (data, più recenti prima).
    val sortCriteria: List<SortCriterion<ListSort>> = emptyList(),
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

    // Ordinamento ricordato dall'ultima volta (issue #9), ora multi-campo a cascata.
    private val _uiState = MutableStateFlow(
        ExpensesListUiState(
            sortCriteria = sortPreferences.getList(SortPreferences.EXPENSES)
                .toSortCriteria { runCatching { ListSort.valueOf(it) }.getOrNull() },
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

    fun onSortToggle(sort: ListSort) {
        _uiState.update { it.copy(sortCriteria = it.sortCriteria.toggled(sort)) }
        // Ricorda la cascata per la prossima apertura (issue #9).
        sortPreferences.setList(SortPreferences.EXPENSES, _uiState.value.sortCriteria.toSaved())
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
        val cmp = state.sortCriteria.comparator<ExpensesList, ListSort> { key ->
            when (key) {
                ListSort.DATE -> { l -> l.createdAt }
                ListSort.NAME -> { l -> l.name.lowercase() }
            }
        } ?: compareByDescending { it.createdAt } // default: più recenti prima
        val sorted = filtered.sortedWith(cmp)
        _uiState.update {
            it.copy(
                activeLists = sorted.filter { l -> !l.paid },
                paidLists = sorted.filter { l -> l.paid },
            )
        }
    }
}
