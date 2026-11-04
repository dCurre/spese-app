package com.dcapps.spese.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Mostra [message] in uno Snackbar quando diventa non-null, poi invoca [onShown] per ripulirlo.
 * Si aggancia a un [SnackbarHostState] fornito dallo Scaffold della schermata.
 */
@Composable
fun MessageSnackbarEffect(
    message: String?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onShown()
        }
    }
}
