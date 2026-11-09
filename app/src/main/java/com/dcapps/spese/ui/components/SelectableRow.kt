package com.dcapps.spese.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Riga con supporto alla SELEZIONE MULTIPLA, condivisa da tutte le liste (evita di duplicare la
 * logica long-press/checkbox/evidenziazione in ogni schermata).
 *
 * - In modalità normale: tap = [onClick], long-press = [onLongClick] (entra in selezione).
 * - In modalità selezione ([selectionMode] = true): a sinistra appare un checkbox, lo sfondo della
 *   riga selezionata è evidenziato, e il tap fa [onToggle] invece di [onClick].
 *
 * @param content il contenuto della riga, reso dopo l'eventuale checkbox (RowScope: può usare weight).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableRow(
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.background,
            )
            .combinedClickable(
                onClick = { if (selectionMode) onToggle() else onClick() },
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        content()
    }
}
