package com.dcapps.spese.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Campo a tendina in sola lettura (ExposedDropdownMenuBox): mostra l'opzione selezionata e, al
 * tocco, un menu con le opzioni. Punto unico al posto delle copie quasi identiche (picker
 * categoria, unità, "chi ha pagato", tipo, ruolo).
 *
 * @param options coppie (id, etichetta). L'id può essere di qualunque tipo [T].
 * @param selectedId id attualmente selezionato (o null).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ReadOnlyPickerField(
    label: String,
    options: List<Pair<T, String>>,
    selectedId: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { onSelect(id); expanded = false },
                )
            }
        }
    }
}
