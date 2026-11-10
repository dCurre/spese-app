package com.dcapps.spese.feature.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.network.ConnectivityObserver
import com.dcapps.spese.core.sync.SyncState
import com.dcapps.spese.core.sync.SyncStatusReporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Stato dell'indicatore di sync in UI. Aggrega:
 * - lo stato di sincronizzazione ([SyncState]: syncing / errore reale);
 * - la connettività ([ConnectivityObserver]).
 *
 * Priorità di visualizzazione: SYNCING (sta allineando) > OFFLINE (nessuna rete, ma non è un bug) >
 * ERROR (ultimo ciclo fallito con errore reale) > nascosto.
 */
enum class SyncIndicatorState { HIDDEN, SYNCING, OFFLINE, ERROR }

@HiltViewModel
class SyncIndicatorViewModel @Inject constructor(
    private val syncStatus: SyncStatusReporter,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    val state: StateFlow<SyncIndicatorState> =
        combine(syncStatus.state, connectivity.isOnline) { sync, online ->
            when {
                sync == SyncState.SYNCING -> SyncIndicatorState.SYNCING
                !online -> SyncIndicatorState.OFFLINE
                sync == SyncState.ERROR -> SyncIndicatorState.ERROR
                else -> SyncIndicatorState.HIDDEN
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncIndicatorState.HIDDEN)

    /** L'utente ha preso atto dell'errore (tap sull'indicatore). */
    fun acknowledgeError() = syncStatus.clearError()
}
