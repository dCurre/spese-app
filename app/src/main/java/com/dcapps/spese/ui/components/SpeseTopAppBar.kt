package com.dcapps.spese.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R

/**
 * TopAppBar dell'app: stesso colore dello sfondo ([surface]) per non staccare dal contenuto.
 * Punto unico per il colore della barra superiore (coerenza + futuro restyling).
 *
 * Se [onTitleEdit] è valorizzato, accanto al titolo appare una matita cliccabile (rinomina):
 * così tutte le sezioni che mostrano il nome di una lista/elemento hanno la rinomina nello
 * stesso posto, invece che nascosta nel menu "⋮".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeseTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    onTitleEdit: (() -> Unit)? = null,
    titleEditLabel: String? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            if (onTitleEdit == null) {
                Text(title)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title)
                    IconButton(onClick = onTitleEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = titleEditLabel ?: stringResource(R.string.detail_rename_list),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
    )
}
