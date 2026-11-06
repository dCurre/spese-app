package com.dcapps.spese.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.dcapps.spese.R

/**
 * Menu "⋮" standard delle top bar di dettaglio: gestisce lo stato aperto/chiuso e l'icona, e
 * passa alle voci una lambda [dismiss] per chiudere il menu dopo l'azione. Punto unico al posto
 * dello scaffold ripetuto (IconButton MoreVert + DropdownMenu) nelle varie schermate.
 *
 * Esempio:
 * ```
 * OverflowMenu { dismiss ->
 *     DropdownMenuItem(text = { Text("Elimina") }, onClick = { dismiss(); onDelete() })
 * }
 * ```
 */
@Composable
fun OverflowMenu(
    content: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val dismiss = { expanded = false }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.detail_more_actions))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = dismiss) {
        content(dismiss)
    }
}
