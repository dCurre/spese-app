package com.dcapps.spese.feature.feedback

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.R
import com.dcapps.spese.ui.components.SpeseDialog

/**
 * Modale per inviare un feedback / richiesta di funzionalità. L'utente descrive la feature
 * che vorrebbe; all'invio viene creata una issue GitHub con label `enhancement`.
 */
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    viewModel: FeedbackViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.reset() }

    // Successo: ringraziamento e chiusura.
    if (state.resultUrl != null) {
        SpeseDialog(
            title = stringResource(R.string.feedback_thanks_title),
            onDismiss = onDismiss,
            content = { Text(stringResource(R.string.feedback_thanks_message)) },
            confirmText = stringResource(R.string.action_close),
            onConfirm = onDismiss,
            dismissButton = {},
        )
        return
    }

    SpeseDialog(
        title = stringResource(R.string.feedback_title),
        onDismiss = onDismiss,
        content = {
            Text(
                stringResource(R.string.feedback_intro),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.feedback_description_label)) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                minLines = 3,
            )
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
        },
        confirmText = if (state.sending) stringResource(R.string.feedback_sending)
            else stringResource(R.string.feedback_send),
        onConfirm = viewModel::send,
        confirmEnabled = !state.sending && !state.notConfigured && state.description.isNotBlank(),
    )
}
