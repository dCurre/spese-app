package com.dcapps.spese.feature.sync

import androidx.lifecycle.ViewModel
import com.dcapps.spese.core.sync.SyncState
import com.dcapps.spese.core.sync.SyncStatusReporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Espone lo stato di sincronizzazione aggregato per il piccolo indicatore in UI. */
@HiltViewModel
class SyncIndicatorViewModel @Inject constructor(
    private val syncStatus: SyncStatusReporter,
) : ViewModel() {
    val state: StateFlow<SyncState> = syncStatus.state

    /** L'utente ha preso atto dell'errore (tap sull'indicatore). */
    fun acknowledgeError() = syncStatus.clearError()
}
