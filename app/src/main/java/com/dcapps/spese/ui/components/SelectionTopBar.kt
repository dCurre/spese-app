package com.dcapps.spese.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dcapps.spese.R
import com.dcapps.spese.ui.theme.SpeseTheme

/**
 * TopAppBar contestuale per la modalità selezione multipla (long-press su una riga): mostra il
 * conteggio degli elementi selezionati, una X per annullare e un cestino per eliminarli.
 * Punto unico, usato in tutte le liste che supportano la selezione multipla.
 *
 * @param count numero di elementi selezionati (mostrato nel titolo).
 * @param onCancel annulla la selezione e torna alla barra normale.
 * @param onDelete chiede l'eliminazione dei selezionati (tipicamente apre una conferma).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    count: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    SpeseTopAppBar(
        title = stringResource(R.string.selection_count, count),
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
            }
        },
        actions = {
            IconButton(onClick = onDelete, enabled = count > 0) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = SpeseTheme.colors.danger,
                )
            }
        },
    )
}
