package com.dcapps.spese.feature.checklist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.R
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.data.ShoppingRepository
import com.dcapps.spese.core.model.CreateShoppingListRequest
import com.dcapps.spese.core.model.ShoppingList
import com.dcapps.spese.core.model.UpdateShoppingListRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.realtime.RealtimeService
import com.dcapps.spese.core.realtime.RealtimeTable
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChecklistSort { DATE, NAME }

data class ChecklistUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val starred: List<ShoppingList> = emptyList(),
    val active: List<ShoppingList> = emptyList(),
    val completed: List<ShoppingList> = emptyList(),
    val query: String = "",
    val sort: ChecklistSort = ChecklistSort.DATE,
    val sortAsc: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class ChecklistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shoppingRepository: ShoppingRepository,
    private val realtimeService: RealtimeService,
    private val session: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    private var allLists: List<ShoppingList> = emptyList()

    init {
        load(showLoading = true)
        viewModelScope.launch {
            realtimeService.watch(RealtimeTable.SHOPPING_LISTS).collect { load() }
        }
    }

    /**
     * Carica le liste. [showLoading] = true mostra lo spinner (solo primo caricamento); reload
     * da realtime/mutazioni usano false (reload silenzioso, niente lampeggio dell'elenco).
     */
    fun load(showLoading: Boolean = false) {
        val userId = session.currentUser.value?.id
        if (userId == null) {
            _uiState.update { it.copy(isLoading = false, error = context.getString(R.string.vm_user_unavailable)) }
            return
        }
        if (showLoading) _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = shoppingRepository.getByUser(userId)) {
                is ApiResult.Success -> {
                    allLists = result.data
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    recompute()
                }
                is ApiResult.Error ->
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(query = q) }
        recompute()
    }

    fun onSortChange(sort: ChecklistSort) {
        _uiState.update {
            if (it.sort == sort) it.copy(sortAsc = !it.sortAsc) else it.copy(sort = sort, sortAsc = false)
        }
        recompute()
    }

    fun create(name: String, shared: Boolean) {
        val userId = session.currentUser.value?.id ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = shoppingRepository.create(
                CreateShoppingListRequest(
                    name = name.trim(),
                    ownerId = userId,
                    listType = if (shared) "shared" else "personal",
                )
            )
            when (result) {
                is ApiResult.Success -> load()
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun toggleStarred(list: ShoppingList) {
        viewModelScope.launch {
            shoppingRepository.update(list.id, UpdateShoppingListRequest(starred = !list.starred))
            load()
        }
    }

    fun delete(list: ShoppingList) {
        viewModelScope.launch {
            when (val result = shoppingRepository.delete(list.id)) {
                is ApiResult.Success -> load()
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun recompute() {
        val q = _uiState.value.query
        val s = _uiState.value
        val filtered = allLists.filter { q.isBlank() || it.name.contains(q, ignoreCase = true) }
        val base: Comparator<ShoppingList> = when (s.sort) {
            ChecklistSort.DATE -> compareBy { it.createdAt }
            ChecklistSort.NAME -> compareBy { it.name.lowercase() }
        }
        val sorted = filtered.sortedWith(if (s.sortAsc) base else base.reversed())
        _uiState.update {
            it.copy(
                starred = sorted.filter { l -> l.starred },
                active = sorted.filter { l -> !l.completed && !l.starred },
                completed = sorted.filter { l -> l.completed && !l.starred },
            )
        }
    }
}
