package com.dcapps.spese.feature.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import com.dcapps.spese.core.update.ChangelogEntry
import com.dcapps.spese.ui.components.SpeseDialog

/**
 * Modale "Novità" mostrata al primo avvio DOPO un aggiornamento: elenca le novità per versione in
 * un box scorrevole ad altezza fissa. Solo informativa: un unico bottone "Ho capito".
 */
@Composable
fun WhatsNewDialog(
    entries: List<ChangelogEntry>,
    onDismiss: () -> Unit,
) {
    SpeseDialog(
        title = stringResource(R.string.update_whats_new),
        onDismiss = onDismiss,
        icon = Icons.Outlined.Celebration,
        content = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                entries.forEach { entry ->
                    Text(
                        entry.version.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                    entry.lines.forEach { line ->
                        Text(
                            "•  $line",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        )
                    }
                }
            }
        },
        // Solo conferma informativa: niente "Annulla".
        confirmText = stringResource(R.string.action_got_it),
        onConfirm = onDismiss,
        dismissButton = {},
    )
}
