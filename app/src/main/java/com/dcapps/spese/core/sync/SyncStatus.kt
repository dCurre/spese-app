package com.dcapps.spese.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Stato aggregato della sincronizzazione, per un piccolo indicatore in UI. */
enum class SyncState {
    /** Nessun sync in corso e nessun errore pendente. */
    IDLE,

    /** Almeno un ciclo di sync è in corso. */
    SYNCING,

    /** L'ultimo ciclo ha incontrato un errore NON atteso (rete transitoria esclusa). */
    ERROR,
}

/**
 * Raccoglie lo stato di sincronizzazione delle varie sezioni (checklist, spese, prezzi) in un unico
 * [state] osservabile dalla UI. Ogni engine segnala inizio/fine ciclo e un eventuale errore reale.
 *
 * Aggregazione: SYNCING se almeno un engine sta sincronizzando; altrimenti ERROR se l'ultimo giro ha
 * avuto un errore non ancora "visto"; altrimenti IDLE. Gli errori di sola rete (offline) NON portano
 * in ERROR: sono attesi e silenziati a monte (vedi runSyncStep), qui arriva solo l'errore reale.
 */
@Singleton
class SyncStatusReporter @Inject constructor() {

    private data class Agg(val running: Int = 0, val hasError: Boolean = false)

    private val agg = MutableStateFlow(Agg())

    private val _state = MutableStateFlow(SyncState.IDLE)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    fun onSyncStarted() {
        agg.update { it.copy(running = it.running + 1) }
        recompute()
    }

    /** Fine di un ciclo di sync di un engine. [failed] = true se ha avuto un errore reale. */
    fun onSyncFinished(failed: Boolean) {
        agg.update { it.copy(running = (it.running - 1).coerceAtLeast(0), hasError = it.hasError || failed) }
        recompute()
    }

    /** La UI chiama questo quando l'utente ha "visto" lo stato di errore (es. dopo un sync manuale). */
    fun clearError() {
        agg.update { it.copy(hasError = false) }
        recompute()
    }

    private fun recompute() {
        val a = agg.value
        _state.value = when {
            a.running > 0 -> SyncState.SYNCING
            a.hasError -> SyncState.ERROR
            else -> SyncState.IDLE
        }
    }
}
