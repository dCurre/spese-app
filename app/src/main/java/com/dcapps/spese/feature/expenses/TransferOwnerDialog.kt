package com.dcapps.spese.feature.expenses

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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
import com.dcapps.spese.ui.components.SpeseDialog

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

    SpeseDialog(
        title = stringResource(R.string.detail_transfer_owner),
        onDismiss = onDismiss,
        icon = Icons.Outlined.SwapHoriz,
        content = {
            if (candidates.isEmpty()) {
                Text(
                    stringResource(R.string.transfer_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                candidates.forEach { p ->
                    Row(
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
        },
        confirmText = stringResource(R.string.transfer_confirm),
        onConfirm = { selected?.let(onConfirm) },
        confirmEnabled = selected != null,
    )
}
