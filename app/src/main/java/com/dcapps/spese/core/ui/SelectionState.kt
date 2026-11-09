package com.dcapps.spese.core.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Stato riutilizzabile per la SELEZIONE MULTIPLA (long-press → selezione → azione) condiviso da
 * tutte le liste. Evita di riscrivere in ogni ViewModel i campi `selectionMode`/`selectedIds` e i
 * metodi start/toggle/clear: il ViewModel istanzia uno di questi e ne espone il [state].
 *
 * L'eliminazione effettiva resta nel ViewModel (ogni entità ha la propria API): legge [ids] da qui.
 */
class SelectionController {
    private val _state = MutableStateFlow(SelectionUiState())
    val state: StateFlow<SelectionUiState> = _state.asStateFlow()

    val ids: Set<Long> get() = _state.value.selectedIds
    val isActive: Boolean get() = _state.value.selectionMode

    /** Entra in selezione (long-press) selezionando subito [id]. */
    fun start(id: Long) = _state.update { SelectionUiState(selectionMode = true, selectedIds = setOf(id)) }

    /** Seleziona/deseleziona [id]; se la selezione si svuota, esce dalla modalità. */
    fun toggle(id: Long) = _state.update {
        val next = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id
        it.copy(selectedIds = next, selectionMode = next.isNotEmpty())
    }

    /** Annulla la selezione. */
    fun clear() = _state.update { SelectionUiState() }
}

/** Snapshot immutabile dello stato di selezione, osservabile dalla UI. */
data class SelectionUiState(
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
)
