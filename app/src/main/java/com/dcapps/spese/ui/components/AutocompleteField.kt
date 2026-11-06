package com.dcapps.spese.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Campo di testo editabile con autocomplete: mostra fino a 5 suggerimenti filtrati da [suggestions]
 * man mano che si digita. Punto unico per i campi con autocomplete (nome spesa, negozio…).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var expanded by remember { mutableStateOf(false) }
    val matches = suggestions.filter {
        value.isNotBlank() && it.contains(value, ignoreCase = true) && !it.equals(value, ignoreCase = true)
    }.distinct().take(5)

    ExposedDropdownMenuBox(
        expanded = expanded && matches.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        DropdownMenu(
            expanded = expanded && matches.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            matches.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s) },
                    onClick = { onValueChange(s); expanded = false },
                )
            }
        }
    }
}
