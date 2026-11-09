package com.dcapps.spese.feature.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.data.ShoppingRepository
import com.dcapps.spese.core.model.CreateShoppingListRequest
import com.dcapps.spese.core.model.ShoppingList
import com.dcapps.spese.core.model.UpdateShoppingListRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.prefs.SortPreferences
import com.dcapps.spese.core.ui.SelectionController
import com.dcapps.spese.core.ui.launchMutation
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val shoppingRepository: ShoppingRepository,
    private val session: SessionManager,
    private val sortPreferences: SortPreferences,
) : ViewModel() {

    // Ordinamento ricordato dall'ultima volta (issue #9).
    private val savedSort = sortPreferences.get(SortPreferences.CHECKLIST_LISTS)
    private val _uiState = MutableStateFlow(
        ChecklistUiState(
            sort = savedSort?.criterion?.let { runCatching { ChecklistSort.valueOf(it) }.getOrNull() }
                ?: ChecklistSort.DATE,
            sortAsc = savedSort?.ascending ?: false,
        ),
    )
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    private var allLists: List<ShoppingList> = emptyList()

    init {
        // Local-first: osserva Room. Ogni modifica (locale o portata dal sync) rinfresca la UI.
        viewModelScope.launch {
            shoppingRepository.observeLists().collect { lists ->
                allLists = lists
                _uiState.update { it.copy(isLoading = false, error = null) }
                recompute()
            }
        }
    }

    /** Compat: con l'osservazione reattiva il refresh manuale non serve più (no-op). */
    fun load(showLoading: Boolean = false) { /* reattivo: niente da fare */ }

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(query = q) }
        recompute()
    }

    fun onSortChange(sort: ChecklistSort) {
        _uiState.update {
            if (it.sort == sort) it.copy(sortAsc = !it.sortAsc) else it.copy(sort = sort, sortAsc = false)
        }
        // Ricorda la scelta per la prossima apertura (issue #9).
        _uiState.value.let { sortPreferences.set(SortPreferences.CHECKLIST_LISTS, it.sort.name, it.sortAsc) }
        recompute()
    }

    fun create(name: String, shared: Boolean) {
        val userId = session.currentUser.value?.id ?: return
        if (name.isBlank()) return
        // Local-first: la create scrive su Room; il Flow osservato aggiorna la UI da solo.
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { },
            call = {
                shoppingRepository.create(
                    CreateShoppingListRequest(
                        name = name.trim(),
                        ownerId = userId,
                        listType = if (shared) "shared" else "personal",
                    )
                )
            },
        )
    }

    fun toggleStarred(list: ShoppingList) {
        viewModelScope.launch {
            shoppingRepository.update(list.id, UpdateShoppingListRequest(starred = !list.starred))
        }
    }

    fun delete(list: ShoppingList) {
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { },
            call = { shoppingRepository.delete(list.id) },
        )
    }

    // --- Selezione multipla (issue #10) ---
    val selection = SelectionController()

    /** Elimina tutte le checklist selezionate in UNA chiamata bulk, poi esce dalla selezione. */
    fun deleteSelected() {
        val ids = selection.ids.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val r = shoppingRepository.deleteBulk(ids)
            selection.clear()
            if (r is ApiResult.Error) _uiState.update { it.copy(message = r.message) }
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
