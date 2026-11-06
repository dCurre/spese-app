package com.dcapps.spese.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.dcapps.spese.R

/** Dialog di conferma generico, usato per le azioni distruttive (eliminazioni). */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(R.string.action_delete),
    icon: ImageVector = Icons.Outlined.WarningAmber,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    SpeseDialog(
        title = title,
        onDismiss = onDismiss,
        icon = icon,
        content = { Text(message) },
        confirmText = confirmText,
        onConfirm = { onConfirm(); onDismiss() },
        destructive = true,
    )
}
