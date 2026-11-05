package com.dcapps.spese.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R

/** Un criterio di ordinamento: una chiave stabile + l'etichetta mostrata all'utente. */
data class SortOption<K>(val key: K, val label: String)

/**
 * Barra di ordinamento riutilizzabile: una riga di chip (uno per criterio). Il chip del criterio
 * attivo mostra la freccia su/giù; ritoccandolo si inverte la direzione, sceglierne un altro lo
 * attiva (in direzione [defaultAscending]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <K> SortBar(
    options: List<SortOption<K>>,
    selectedKey: K,
    ascending: Boolean,
    onSelect: (K) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val selected = option.key == selectedKey
            FilterChip(
                selected = selected,
                onClick = { onSelect(option.key) },
                label = { androidx.compose.material3.Text(option.label) },
                trailingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = if (ascending) Icons.Default.ArrowUpward
                            else Icons.Default.ArrowDownward,
                            contentDescription = if (ascending) stringResource(R.string.sort_ascending) else stringResource(R.string.sort_descending),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else null,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}
