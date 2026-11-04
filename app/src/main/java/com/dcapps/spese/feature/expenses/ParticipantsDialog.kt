package com.dcapps.spese.feature.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.dcapps.spese.core.model.User

/**
 * Dialog di gestione partecipanti: elenco attuale (con rimozione), aggiunta di un utente
 * registrato e creazione di un ospite senza account.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantsDialog(
    participants: List<ExpensesListParticipant>,
    availableUsers: List<User>,
    ownerId: Long?,
    onAddUser: (Long) -> Unit,
    onAddGuest: (String) -> Unit,
    onRemove: (ExpensesListParticipant) -> Unit,
    onDismiss: () -> Unit,
) {
    var guestName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Partecipanti") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // Elenco partecipanti attuali.
                participants.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val label = buildString {
                            append(participantLabel(p))
                            if (p.isGuest) append(" (ospite)")
                            if (p.userId == ownerId) append(" · proprietario")
                        }
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        // Il proprietario non si può rimuovere.
                        if (p.userId != ownerId) {
                            IconButton(onClick = { onRemove(p) }) {
                                Icon(Icons.Default.Close, contentDescription = "Rimuovi")
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // Aggiungi utente registrato.
                if (availableUsers.isNotEmpty()) {
                    Text("Aggiungi utente", style = MaterialTheme.typography.labelLarge)
                    availableUsers.forEach { u ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(u.displayName, style = MaterialTheme.typography.bodyMedium)
                            AssistChip(
                                onClick = { onAddUser(u.id) },
                                label = { Text("Aggiungi") },
                                leadingIcon = {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                },
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }

                // Aggiungi ospite.
                Text("Aggiungi ospite", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = guestName,
                        onValueChange = { guestName = it },
                        placeholder = { Text("Nome ospite") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        enabled = guestName.isNotBlank(),
                        onClick = { onAddGuest(guestName); guestName = "" },
                    ) { Text("Crea") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
    )
}
