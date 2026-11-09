package com.dcapps.spese.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.network.ApiResult
import kotlinx.coroutines.launch

/**
 * Incapsula il pattern ripetuto in quasi tutti i ViewModel per le mutazioni di rete:
 *
 *     viewModelScope.launch {
 *         when (val r = repo.xxx()) {
 *             is Success -> onSuccess(r.data)
 *             is Error   -> onError(r)   // di norma: mostra r.message nello UiState
 *         }
 *     }
 *
 * Non impone una base class né uno stato condiviso (le data class UiState restano quelle di ogni
 * schermata): il VM passa solo cosa fare al successo e come mostrare l'errore. [onError] riceve
 * l'intero [ApiResult.Error] così il chiamante può usare `message`, `code` o `isExpected`.
 */
fun <T> ViewModel.launchMutation(
    onError: (ApiResult.Error) -> Unit,
    onSuccess: (T) -> Unit,
    call: suspend () -> ApiResult<T>,
) {
    viewModelScope.launch {
        when (val result = call()) {
            is ApiResult.Success -> onSuccess(result.data)
            is ApiResult.Error -> onError(result)
        }
    }
}
