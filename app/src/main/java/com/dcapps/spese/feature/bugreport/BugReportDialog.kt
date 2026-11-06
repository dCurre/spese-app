package com.dcapps.spese.feature.bugreport

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dcapps.spese.R
import com.dcapps.spese.ui.components.SpeseDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Dialog di segnalazione bug, proposto al riavvio dopo un crash. Mostra lo stacktrace
 * (sola lettura) e permette all'utente di aggiungere una descrizione e inviare la issue.
 */
@Composable
fun BugReportDialog(
    onDismiss: () -> Unit,
    viewModel: BugReportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Successo: mostra conferma e chiude.
    if (state.resultUrl != null) {
        SpeseDialog(
            title = stringResource(R.string.bug_thanks_title),
            onDismiss = onDismiss,
            content = { Text(stringResource(R.string.bug_thanks_message)) },
            confirmText = stringResource(R.string.action_close),
            onConfirm = onDismiss,
            dismissButton = {},
        )
        return
    }

    SpeseDialog(
        title = stringResource(R.string.bug_title),
        onDismiss = onDismiss,
        content = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.bug_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text(stringResource(R.string.bug_description_label)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    minLines = 2,
                )
                if (state.stackTrace != null) {
                    Text(
                        stringResource(R.string.bug_tech_included),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        state.stackTrace!!.take(1000),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState()),
                    )
                }
                if (state.notConfigured) {
                    Text(
                        stringResource(R.string.bug_not_configured),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                state.error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmText = if (state.sending) stringResource(R.string.bug_sending)
            else stringResource(R.string.bug_send),
        onConfirm = viewModel::send,
        confirmEnabled = !state.sending && !state.notConfigured,
        // "No grazie" scarta anche il crash salvato: dismiss custom ma neutro come l'annulla.
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = { viewModel.dismissCrash(); onDismiss() }) {
                Text(stringResource(R.string.bug_no_thanks))
            }
        },
    )
}
