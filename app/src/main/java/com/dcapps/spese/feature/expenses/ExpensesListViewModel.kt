package com.dcapps.spese.feature.expenses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.R
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.data.ExpensesRepository
import com.dcapps.spese.core.data.UserRepository
import com.dcapps.spese.core.realtime.RealtimeService
import com.dcapps.spese.core.realtime.RealtimeTable
import com.dcapps.spese.core.model.CreateExpensesListRequest
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.core.model.UpdateExpensesListRequest
import com.dcapps.spese.core.model.UpdateUserRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.prefs.SortPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
)

@HiltViewModel
class ExpensesListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val expensesRepository: ExpensesRepository,
    private val userRepository: UserRepository,
    private val realtimeService: RealtimeService,
    private val session: SessionManager,
    private val sortPreferences: SortPreferences,
) : ViewModel() {

    // Ordinamento ricordato dall'ultima volta (issue #9).
    private val savedSort = sortPreferences.get(SortPreferences.EXPENSES)
    private val _uiState = MutableStateFlow(
        ExpensesListUiState(
            sort = savedSort?.criterion?.let { runCatching { ListSort.valueOf(it) }.getOrNull() }
                ?: ListSort.DATE,
            sortAsc = savedSort?.ascending ?: false,
        ),
    )
    val uiState: StateFlow<ExpensesListUiState> = _uiState.asStateFlow()

    private var allLists: List<ExpensesList> = emptyList()

    init {
        load(showLoading = true)
        viewModelScope.launch {
            realtimeService.watch(RealtimeTable.EXPENSES_LISTS).collect { load() }
        }
    }

    /**
     * Carica le liste. [showLoading] = true mostra lo spinner (solo primo caricamento); reload
     * da realtime/mutazioni usano false (reload silenzioso, niente lampeggio dell'elenco).
     */
    fun load(showLoading: Boolean = false) {
        val email = session.currentUser.value?.email
        if (email.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, error = context.getString(R.string.vm_user_unavailable)) }
            return
        }
        if (showLoading) _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = expensesRepository.getUserWithLists(email)) {
                is ApiResult.Success -> {
                    // Aggiorna l'utente in sessione (come fa la webapp).
                    session.setUser(result.data.user)
                    allLists = result.data.expensesLists
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            paidListsShown = result.data.user.paidListShown,
                        )
                    }
                    recompute()
                }
                is ApiResult.Error ->
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

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
        val user = session.currentUser.value ?: return
        val newValue = !_uiState.value.paidListsShown
        _uiState.update { it.copy(paidListsShown = newValue) }
        viewModelScope.launch {
            userRepository.update(user.id, UpdateUserRequest(paidListShown = newValue))
            session.setUser(user.copy(paidListShown = newValue))
        }
    }

    fun createList(name: String, shared: Boolean) {
        val user = session.currentUser.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val listType = if (shared) "shared" else "personal"
            when (val created = expensesRepository.createList(
                CreateExpensesListRequest(name = name.trim(), userId = user.id, listType = listType)
            )) {
                is ApiResult.Success -> {
                    val newId = created.data
                    if (shared && newId != null) {
                        expensesRepository.addParticipant(newId, user.id)
                    }
                    load()
                }
                is ApiResult.Error ->
                    _uiState.update { it.copy(message = created.message) }
            }
        }
    }

    fun deleteList(list: ExpensesList) {
        viewModelScope.launch {
            when (val result = expensesRepository.deleteList(list.id)) {
                is ApiResult.Success -> load()
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun setPaid(list: ExpensesList, paid: Boolean) {
        viewModelScope.launch {
            expensesRepository.updateList(
                list.id,
                UpdateExpensesListRequest(paid = paid),
            )
            load()
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
