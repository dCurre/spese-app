package com.dcapps.spese.feature.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.data.ExpensesRepository
import com.dcapps.spese.core.data.UserRepository
import com.dcapps.spese.core.realtime.RealtimeService
import com.dcapps.spese.core.realtime.RealtimeTable
import com.dcapps.spese.core.model.CreateExpensesListRequest
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.core.model.UpdateUserRequest
import com.dcapps.spese.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ListSort { DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC }

data class ExpensesListUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val activeLists: List<ExpensesList> = emptyList(),
    val paidLists: List<ExpensesList> = emptyList(),
    val query: String = "",
    val sort: ListSort = ListSort.DATE_DESC,
    val paidListsShown: Boolean = true,
    val message: String? = null,
)

@HiltViewModel
class ExpensesListViewModel @Inject constructor(
    private val expensesRepository: ExpensesRepository,
    private val userRepository: UserRepository,
    private val realtimeService: RealtimeService,
    private val session: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesListUiState())
    val uiState: StateFlow<ExpensesListUiState> = _uiState.asStateFlow()

    private var allLists: List<ExpensesList> = emptyList()

    init {
        load()
        viewModelScope.launch {
            realtimeService.watch(RealtimeTable.EXPENSES_LISTS).collect { load() }
        }
    }

    fun load() {
        val email = session.currentUser.value?.email
        if (email.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Utente non disponibile") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
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
        _uiState.update { it.copy(sort = sort) }
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
                com.dcapps.spese.core.model.UpdateExpensesListRequest(paid = paid),
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
        val comparator: Comparator<ExpensesList> = when (state.sort) {
            ListSort.DATE_DESC -> compareByDescending { it.createdAt }
            ListSort.DATE_ASC -> compareBy { it.createdAt }
            ListSort.NAME_ASC -> compareBy { it.name.lowercase() }
            ListSort.NAME_DESC -> compareByDescending { it.name.lowercase() }
        }
        val sorted = filtered.sortedWith(comparator)
        _uiState.update {
            it.copy(
                activeLists = sorted.filter { l -> !l.paid },
                paidLists = sorted.filter { l -> l.paid },
            )
        }
    }
}
