package com.dcapps.spese.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Contenuto standard di una riga-elenco: titolo in evidenza + un sottotitolo libero (badge, conteggi
 * con icona, ecc.), con uno slot [trailing] a destra (stella, freccetta, prezzo, …). Punto unico al
 * posto dello scaffold riga ripetuto in checklist/spese/prezzi; va usato DENTRO una riga cliccabile
 * (es. [SelectableRow]), che fornisce il `RowScope`.
 *
 * @param subtitle contenuto del sottotitolo, reso in una Row sotto il titolo (può essere vuoto).
 * @param trailing contenuto a destra, oltre la colonna di testo (es. icona stella o chevron).
 */
@Composable
fun RowScope.ListRowContent(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier = Modifier.weight(1f).then(modifier)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (subtitle != null) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = subtitle,
            )
        }
    }
    trailing?.invoke()
}
