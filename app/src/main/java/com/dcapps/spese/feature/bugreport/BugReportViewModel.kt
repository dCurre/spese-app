package com.dcapps.spese.feature.bugreport

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.bugreport.BugReportRepository
import com.dcapps.spese.core.bugreport.CrashStore
import com.dcapps.spese.core.bugreport.ErrorReporter
import com.dcapps.spese.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BugReportUiState(
    val description: String = "",
    val stackTrace: String? = null,
    val sending: Boolean = false,
    val resultUrl: String? = null,
    val error: String? = null,
    val notConfigured: Boolean = false,
)

@HiltViewModel
class BugReportViewModel @Inject constructor(
    private val repository: BugReportRepository,
    private val errorReporter: ErrorReporter,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BugReportUiState())
    val uiState: StateFlow<BugReportUiState> = _uiState.asStateFlow()

    /** Precompila con l'ultimo errore noto: crash salvato su disco o ultimo errore runtime. */
    fun prefillFromLastError() {
        val crash = CrashStore.read(context)
        val trace = crash ?: errorReporter.lastError?.stackTrace
        _uiState.update {
            it.copy(
                stackTrace = trace,
                notConfigured = !repository.isConfigured,
            )
        }
    }

    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }

    fun send() {
        val state = _uiState.value
        if (!repository.isConfigured) {
            _uiState.update { it.copy(notConfigured = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(sending = true, error = null) }
            // Il crash salvato su disco non ha contesto di rete; l'ultimo errore runtime sì.
            val netError = if (CrashStore.read(context) == null) errorReporter.lastError else null
            when (val r = repository.report(state.description, state.stackTrace, netError)) {
                is ApiResult.Success -> {
                    CrashStore.clear(context)
                    errorReporter.clear()
                    _uiState.update { it.copy(sending = false, resultUrl = r.data) }
                }
                is ApiResult.Error ->
                    _uiState.update { it.copy(sending = false, error = r.message) }
            }
        }
    }

    /** L'utente ha scartato la proposta post-crash: rimuove il crash salvato. */
    fun dismissCrash() {
        CrashStore.clear(context)
    }
}
