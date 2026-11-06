package com.dcapps.spese.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.dcapps.spese.R
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
    val reportLabel = stringResource(R.string.snackbar_report)
    LaunchedEffect(message) {
        if (message != null) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (onReport != null) reportLabel else null,
                // Mostra la "X" per chiudere il toast in anticipo (oltre all'eventuale "Segnala").
                withDismissAction = true,
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
    // false per gli errori applicativi attesi (permessi/validazione): niente azione "Segnala",
    // perché non sono bug da segnalare ma risposte legittime del backend.
    reportable: Boolean = true,
    gateViewModel: BugReportGateViewModel = hiltViewModel(),
) {
    MessageSnackbarEffect(
        message = message,
        snackbarHostState = snackbarHostState,
        onShown = onShown,
        onReport = if (reportable) ({ gateViewModel.request() }) else null,
    )
}
