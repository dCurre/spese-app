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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import com.dcapps.spese.core.prefs.SortCriterion

/** Un criterio di ordinamento: una chiave stabile + l'etichetta mostrata all'utente. */
data class SortOption<K>(val key: K, val label: String)

/**
 * Barra di ordinamento MULTI-CAMPO a cascata: una riga di chip (uno per criterio disponibile).
 * L'utente tocca i criteri nell'ordine di priorità che vuole; ogni chip attivo mostra il suo numero
 * di priorità (1-based) e la freccia della sua direzione. Il tap è un ciclo a 3 stati
 * (aggiungi asc → desc → rimuovi), la cui logica sta in [SortCriterion.toggled] lato ViewModel.
 *
 * Nessun limite al numero di criteri attivi: la cascata può includere tutti i campi disponibili.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <K> SortBar(
    options: List<SortOption<K>>,
    criteria: List<SortCriterion<K>>,
    onToggle: (K) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val index = criteria.indexOfFirst { it.key == option.key }
            val selected = index >= 0
            val ascending = if (selected) criteria[index].ascending else true
            FilterChip(
                selected = selected,
                onClick = { onToggle(option.key) },
                label = {
                    // Sul chip attivo prefissa il numero di priorità (es. "1 · Nome").
                    Text(if (selected) "${index + 1} · ${option.label}" else option.label)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
                // Bordo più contrastato dei default Material (poco visibili su sfondo scuro).
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MaterialTheme.colorScheme.outline,
                    borderWidth = 1.dp,
                ),
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
