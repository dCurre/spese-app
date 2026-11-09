package com.dcapps.spese.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import com.dcapps.spese.ui.components.SpeseConfirmButton
import com.dcapps.spese.ui.components.SpeseDialog

/**
 * Chiede all'ospite, dopo il login, cosa fare dei dati creati in locale: [onMerge] li aggiunge
 * all'account (ricreati via sync), [onOnlineOnly] li scarta e usa solo i dati online. Vale per tutte
 * le sezioni (checklist, spese, prezzi). Non è dismissibile: la scelta è obbligatoria.
 */
@Composable
fun MigrationChoiceDialog(
    inProgress: Boolean,
    onMerge: () -> Unit,
    onOnlineOnly: () -> Unit,
) {
    SpeseDialog(
        title = stringResource(R.string.migration_title),
        icon = Icons.Default.CloudSync,
        // Scelta obbligatoria: tap fuori/back non chiudono (onDismiss no-op).
        onDismiss = {},
        content = {
            if (inProgress) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(12.dp))
                    Text(stringResource(R.string.migration_in_progress))
                }
            } else {
                Text(stringResource(R.string.migration_message))
            }
        },
        // "Unisci" = azione primaria (verde); "Solo online" = azione neutra a sinistra.
        confirmButton = {
            SpeseConfirmButton(
                text = stringResource(R.string.migration_merge),
                onClick = onMerge,
                enabled = !inProgress,
            )
        },
        dismissButton = {
            TextButton(onClick = onOnlineOnly, enabled = !inProgress) {
                Text(stringResource(R.string.migration_online_only))
            }
        },
    )
}
