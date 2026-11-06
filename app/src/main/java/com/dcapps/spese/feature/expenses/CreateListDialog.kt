package com.dcapps.spese.feature.expenses

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
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
    @StringRes titleRes: Int = R.string.lists_new,
) {
    var name by remember { mutableStateOf("") }
    var shared by remember { mutableStateOf(true) }

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
            TypeOption(stringResource(R.string.lists_type_shared), selected = shared) { shared = true }
            TypeOption(stringResource(R.string.lists_type_personal), selected = !shared) { shared = false }
        },
        confirmText = stringResource(R.string.action_create),
        onConfirm = { onCreate(name, shared) },
        confirmEnabled = name.isNotBlank(),
    )
}

@Composable
private fun TypeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
