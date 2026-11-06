package com.dcapps.spese.feature.checklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.dcapps.spese.ui.theme.SpeseTheme

/**
 * Rende l'alberatura grafica della checklist: per ogni livello di profondità disegna una linea
 * verticale guida a sinistra (come il frontend), così l'annidamento di categorie e item è visibile
 * e non affidato al solo padding. Righe consecutive allo stesso livello formano una linea continua.
 *
 * @param depth profondità dell'elemento (0 = radice, nessuna linea).
 * @param content contenuto della riga, reso a destra delle guide.
 */
@Composable
fun TreeIndent(
    depth: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (depth <= 0) {
        // Nessuna guida da disegnare, ma il modifier (es. il .clickable della riga) va comunque
        // applicato: avvolgiamo il contenuto in un Box che lo riceve, altrimenti gli elementi di
        // profondità 0 (es. item senza categoria) perderebbero il click.
        Box(modifier) { content() }
        return
    }
    val lineColor = SpeseTheme.colors.divider
    // L'altezza intrinseca fa sì che le linee coprano esattamente l'altezza della riga,
    // così tra righe adiacenti la guida verticale appare continua.
    Row(modifier = modifier.height(IntrinsicSize.Min)) {
        repeat(depth) { level ->
            // L'ultimo livello (adiacente al contenuto) ha anche un trattino orizzontale "a ramo"
            // (├─) che collega la guida verticale alla riga, come nell'alberatura del frontend.
            val isConnector = level == depth - 1
            Box(
                Modifier
                    .width(16.dp)
                    .fillMaxHeight()
                    .drawBehind {
                        val x = 8.dp.toPx()
                        drawLine(
                            color = lineColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                        if (isConnector) {
                            val midY = size.height / 2f
                            drawLine(
                                color = lineColor,
                                start = Offset(x, midY),
                                end = Offset(size.width, midY),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                    },
            )
        }
        content()
    }
}
