package com.dcapps.spese.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.dcapps.spese.feature.bugreport.BugReportGateViewModel

/**
 * Mostra [message] in uno Snackbar quando diventa non-null, poi invoca [onShown] per ripulirlo.
 * Se [onReport] è fornito, la snackbar mostra un'azione "Segnala": premendola si invoca
 * [onReport] (tipicamente apre il dialog di segnalazione bug precompilato con l'ultimo errore).
 */
@Composable
fun MessageSnackbarEffect(
    message: String?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
    onReport: (() -> Unit)? = null,
) {
    LaunchedEffect(message) {
        if (message != null) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (onReport != null) "Segnala" else null,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) onReport?.invoke()
            onShown()
        }
    }
}

/**
 * Variante che mostra sempre l'azione "Segnala" per gli errori applicativi: al tap apre il
 * dialog di segnalazione bug (gestito globalmente da MainActivity). Da usare per i messaggi
 * d'errore delle chiamate di rete.
 */
@Composable
fun ErrorSnackbarEffect(
    message: String?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
    gateViewModel: BugReportGateViewModel = hiltViewModel(),
) {
    MessageSnackbarEffect(
        message = message,
        snackbarHostState = snackbarHostState,
        onShown = onShown,
        onReport = { gateViewModel.request() },
    )
}
