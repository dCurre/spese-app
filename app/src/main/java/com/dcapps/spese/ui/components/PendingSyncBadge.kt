package com.dcapps.spese.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R

/**
 * Piccola nuvoletta tenue che segnala una riga (lista/articolo/spesa/prodotto) con modifiche locali
 * non ancora sincronizzate col backend. Mostrata solo se [show] è true — il chiamante combina lo
 * stato `pendingSync` della riga con la preferenza utente (vedi `UiPreferences.showPendingSyncBadges`).
 */
@Composable
fun PendingSyncBadge(show: Boolean, modifier: Modifier = Modifier) {
    if (!show) return
    Icon(
        Icons.Outlined.CloudQueue,
        contentDescription = stringResource(R.string.sync_pending_cd),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(16.dp),
    )
}
