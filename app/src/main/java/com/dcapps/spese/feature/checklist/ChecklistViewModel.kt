package com.dcapps.spese.feature.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.data.ShoppingRepository
import com.dcapps.spese.core.model.CreateShoppingListRequest
import com.dcapps.spese.core.model.ShoppingList
import com.dcapps.spese.core.model.UpdateShoppingListRequest
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

enum class ChecklistSort { DATE, NAME }

data class ChecklistUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val starred: List<ShoppingList> = emptyList(),
    val active: List<ShoppingList> = emptyList(),
    val completed: List<ShoppingList> = emptyList(),
    val query: String = "",
    // Ordinamento multi-campo a cascata (ordine = priorità). Vuoto = default (data, più recenti prima).
    val sortCriteria: List<SortCriterion<ChecklistSort>> = emptyList(),
    val message: String? = null,
    // Mostra il badge "in attesa di sync" sulle righe (preferenza utente).
    val showPendingSync: Boolean = true,
    // Connesso a Internet: offline la creazione di liste condivise è lucchettata (punto 4).
    val isOnline: Boolean = true,
)

@HiltViewModel
class ChecklistViewModel @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val session: SessionManager,
    private val sortPreferences: SortPreferences,
    private val uiPreferences: UiPreferences,
    private val connectivity: ConnectivityObserver,
) : ViewModel() {

    // Ordinamento ricordato dall'ultima volta (issue #9), ora multi-campo a cascata.
    private val _uiState = MutableStateFlow(
        ChecklistUiState(
            sortCriteria = sortPreferences.getList(SortPreferences.CHECKLIST_LISTS)
                .toSortCriteria { runCatching { ChecklistSort.valueOf(it) }.getOrNull() },
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
        // Preferenza badge sync + stato connettività (reattivi).
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

    val isGuest: Boolean get() = session.isGuest

    /** Compat: con l'osservazione reattiva il refresh manuale non serve più (no-op). */
    fun load(showLoading: Boolean = false) { /* reattivo: niente da fare */ }

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(query = q) }
        recompute()
    }

    fun onSortToggle(sort: ChecklistSort) {
        _uiState.update { it.copy(sortCriteria = it.sortCriteria.toggled(sort)) }
        // Ricorda la cascata per la prossima apertura (issue #9).
        sortPreferences.setList(SortPreferences.CHECKLIST_LISTS, _uiState.value.sortCriteria.toSaved())
        recompute()
    }

    fun create(name: String, shared: Boolean) {
        val userId = session.currentUser.value?.id ?: return
        if (name.isBlank()) return
        // Difesa: offline la condivisione non è disponibile → forza personale (punto 4).
        val effectiveShared = shared && connectivity.isOnline.value
        // Local-first: la create scrive su Room; il Flow osservato aggiorna la UI da solo.
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { },
            call = {
                shoppingRepository.create(
                    CreateShoppingListRequest(
                        name = name.trim(),
                        ownerId = userId,
                        listType = if (effectiveShared) "shared" else "personal",
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
        val cmp = s.sortCriteria.comparator<ShoppingList, ChecklistSort> { key ->
            when (key) {
                ChecklistSort.DATE -> { l -> l.createdAt }
                ChecklistSort.NAME -> { l -> l.name.lowercase() }
            }
        } ?: compareByDescending { it.createdAt } // default: più recenti prima
        val sorted = filtered.sortedWith(cmp)
        _uiState.update {
            it.copy(
                starred = sorted.filter { l -> l.starred },
                active = sorted.filter { l -> !l.completed && !l.starred },
                completed = sorted.filter { l -> l.completed && !l.starred },
            )
        }
    }
}
