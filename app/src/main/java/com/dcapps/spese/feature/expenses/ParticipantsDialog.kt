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
import androidx.compose.material.icons.outlined.Group
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import com.dcapps.spese.core.model.ExpensesListParticipant
import com.dcapps.spese.core.model.User
import com.dcapps.spese.ui.components.SpeseBottomSheet

/**
 * Gestione partecipanti come bottom sheet: elenco attuale (con rimozione), aggiunta di un utente
 * registrato e creazione di un ospite senza account. Si chiude con swipe-giù / tap-fuori / back.
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

    SpeseBottomSheet(
        title = stringResource(R.string.detail_participants),
        onDismiss = onDismiss,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 480.dp)) {
                // Elenco partecipanti attuali.
                participants.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val guestSuffix = stringResource(R.string.participants_guest_suffix)
                        val ownerSuffix = stringResource(R.string.participants_owner_suffix)
                        val label = buildString {
                            append(participantLabel(p))
                            if (p.isGuest) append(" $guestSuffix")
                            if (p.userId == ownerId) append(" · $ownerSuffix")
                        }
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        // Il proprietario non si può rimuovere.
                        if (p.userId != ownerId) {
                            IconButton(onClick = { onRemove(p) }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.participants_remove))
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // Aggiungi utente registrato.
                if (availableUsers.isNotEmpty()) {
                    Text(stringResource(R.string.participants_add_user), style = MaterialTheme.typography.labelLarge)
                    availableUsers.forEach { u ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(u.displayName, style = MaterialTheme.typography.bodyMedium)
                            AssistChip(
                                onClick = { onAddUser(u.id) },
                                label = { Text(stringResource(R.string.action_add)) },
                                leadingIcon = {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                },
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }

                // Aggiungi ospite.
                Text(stringResource(R.string.participants_add_guest), style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = guestName,
                        onValueChange = { guestName = it },
                        placeholder = { Text(stringResource(R.string.participants_guest_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        enabled = guestName.isNotBlank(),
                        onClick = { onAddGuest(guestName); guestName = "" },
                    ) { Text(stringResource(R.string.action_create)) }
                }
            }
        }
    }
