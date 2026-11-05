package com.dcapps.spese.feature.update

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import com.dcapps.spese.core.update.UpdateInfo

/**
 * Popup di aggiornamento. Mostra versione e changelog; il pulsante scarica e installa l'APK.
 * Se manca il permesso di installazione, indirizza l'utente alle impostazioni.
 */
@Composable
fun UpdateDialog(
    state: UpdateUiState,
    info: UpdateInfo,
    canInstall: Boolean,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!state.downloading) onDismiss() },
        title = { Text(stringResource(R.string.update_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.update_available, info.version),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (info.changelog.isNotBlank()) {
                    Text(
                        info.changelog,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .heightIn(max = 220.dp),
                    )
                }
                when {
                    state.downloading -> {
                        Text(
                            stringResource(R.string.update_downloading, state.progress),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    }
                    state.error != null -> Text(
                        state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (!state.downloading) {
                TextButton(
                    onClick = {
                        if (canInstall) {
                            onUpdate()
                        } else {
                            // Porta l'utente a concedere "installa app sconosciute" per questa app.
                            val intent = Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:${context.packageName}"),
                            )
                            context.startActivity(intent)
                        }
                    },
                ) {
                    Text(
                        if (canInstall) stringResource(R.string.update_action)
                        else stringResource(R.string.update_allow_install),
                    )
                }
            }
        },
        dismissButton = {
            if (!state.downloading) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
            }
        },
    )
}
