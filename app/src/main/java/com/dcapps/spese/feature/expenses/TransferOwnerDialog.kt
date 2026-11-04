package com.dcapps.spese.feature.expenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dcapps.spese.core.model.ExpensesListParticipant

/**
 * Dialog per trasferire la proprietà della lista: si può scegliere solo un partecipante
 * registrato (non ospite) diverso dal proprietario attuale.
 */
@Composable
fun TransferOwnerDialog(
    participants: List<ExpensesListParticipant>,
    ownerId: Long?,
    onConfirm: (newOwnerId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val candidates = participants.filter { it.userId != ownerId && !it.isGuest }
    var selected by remember { mutableStateOf(candidates.firstOrNull()?.userId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trasferisci proprietà") },
        text = {
            if (candidates.isEmpty()) {
                Text(
                    "Nessun partecipante idoneo. Aggiungi prima un utente registrato alla lista.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column {
                    candidates.forEach { p ->
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected == p.userId,
                                    onClick = { selected = p.userId },
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected == p.userId,
                                onClick = { selected = p.userId },
                            )
                            Text(participantLabel(p), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null,
                onClick = { selected?.let(onConfirm) },
            ) { Text("Trasferisci") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
