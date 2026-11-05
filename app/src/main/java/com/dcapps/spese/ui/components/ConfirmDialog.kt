package com.dcapps.spese.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dcapps.spese.R

/** Dialog di conferma generico, usato per le azioni distruttive (eliminazioni). */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(R.string.action_delete),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(); onDismiss() },
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = com.dcapps.spese.ui.theme.SpeseTheme.colors.danger,
                ),
            ) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
