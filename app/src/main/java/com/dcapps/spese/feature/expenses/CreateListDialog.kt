package com.dcapps.spese.feature.expenses

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.dcapps.spese.ui.components.SpeseFormBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, shared: Boolean) -> Unit,
    // L'opzione "condivisa" è lucchettata quando la condivisione non è disponibile: in guest
    // (serve un account) oppure offline (serve la rete). Il chiamante decide il motivo via
    // [onSharedLocked], che mostra il messaggio giusto.
    sharedLocked: Boolean = false,
    onSharedLocked: () -> Unit = {},
    @StringRes titleRes: Int = R.string.lists_new,
) {
    var name by remember { mutableStateOf("") }
    // Se la condivisione è lucchettata, default a personale.
    var shared by remember { mutableStateOf(!sharedLocked) }

    // Pannello dal basso (bottom sheet) invece del dialog centrale: aperto dal "+" della navbar.
    SpeseFormBottomSheet(
        title = stringResource(titleRes),
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TypeOption(
                label = stringResource(R.string.lists_type_shared),
                selected = shared,
                locked = sharedLocked,
                onSelect = { if (sharedLocked) onSharedLocked() else shared = true },
            )
            TypeOption(
                label = stringResource(R.string.lists_type_personal),
                selected = !shared,
                locked = false,
                onSelect = { shared = false },
            )
        },
        confirmText = stringResource(R.string.action_create),
        // Se lucchettata forziamo personale a prescindere dallo stato (difesa extra).
        onConfirm = { onCreate(name, if (sharedLocked) false else shared) },
        confirmEnabled = name.isNotBlank(),
    )
}

@Composable
private fun TypeOption(label: String, selected: Boolean, locked: Boolean, onSelect: () -> Unit) {
    val textColor = if (locked) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, enabled = !locked, onClick = onSelect)
        Text(label, modifier = Modifier.weight(1f).padding(start = 8.dp), color = textColor)
        if (locked) {
            Icon(
                Icons.Default.Lock,
                contentDescription = stringResource(R.string.cd_locked),
                tint = textColor,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
