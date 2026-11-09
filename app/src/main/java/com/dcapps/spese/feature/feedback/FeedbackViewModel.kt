package com.dcapps.spese.feature.feedback

import androidx.lifecycle.ViewModel
import com.dcapps.spese.core.feedback.FeedbackRepository
import com.dcapps.spese.core.ui.launchMutation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class FeedbackUiState(
    val description: String = "",
    val sending: Boolean = false,
    val resultUrl: String? = null,
    val error: String? = null,
    val notConfigured: Boolean = false,
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val repository: FeedbackRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }

    fun send() {
        val state = _uiState.value
        if (!repository.isConfigured) {
            _uiState.update { it.copy(notConfigured = true) }
            return
        }
        if (state.description.isBlank() || state.sending) return
        _uiState.update { it.copy(sending = true, error = null) }
        launchMutation(
            onError = { e -> _uiState.update { it.copy(sending = false, error = e.message) } },
            onSuccess = { url -> _uiState.update { it.copy(sending = false, resultUrl = url) } },
            call = { repository.report(state.description) },
        )
    }

    /** Reset dello stato quando si riapre la modale. */
    fun reset() = _uiState.update { FeedbackUiState(notConfigured = !repository.isConfigured) }
}
