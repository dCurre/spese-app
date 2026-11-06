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

/** Larghezza di una colonna di indentazione: pari al centro della freccetta di collasso (⌄). */
private val INDENT = 28.dp

/**
 * Rende l'alberatura grafica della checklist, fedele al frontend:
 *  - una guida VERTICALE per ogni livello di annidamento, allineata al CENTRO della freccetta di
 *    collasso (⌄) della categoria genitore (la larghezza colonna [INDENT] è scelta per questo);
 *  - sull'ultimo livello, un trattino ORIZZONTALE che si stacca dalla verticale verso la riga;
 *  - per l'ULTIMO figlio del gruppo la verticale si ferma a metà (└─), per gli altri prosegue (├─);
 *  - gli antenati mantengono la loro verticale continua lungo tutta l'altezza.
 *
 * @param depth profondità (0 = radice, nessuna guida).
 * @param isLastChild true se è l'ultimo figlio del suo gruppo (connettore └─ invece di ├─).
 */
@Composable
fun TreeIndent(
    depth: Int,
    modifier: Modifier = Modifier,
    isLastChild: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (depth <= 0) {
        Box(modifier) { content() }
        return
    }
    val lineColor = SpeseTheme.colors.divider
    Row(modifier = modifier.height(IntrinsicSize.Min)) {
        repeat(depth) { level ->
            val isLast = level == depth - 1
            Box(
                Modifier
                    .width(INDENT)
                    .fillMaxHeight()
                    .drawBehind {
                        // Centro della colonna = centro della freccetta del genitore di quel livello.
                        val x = size.width / 2f
                        val midY = size.height / 2f
                        val stroke = 1.dp.toPx()
                        if (isLast) {
                            // Verticale fino a metà (└─) se ultimo figlio, altrimenti tutta (├─).
                            val vEnd = if (isLastChild) midY else size.height
                            drawLine(lineColor, Offset(x, 0f), Offset(x, vEnd), strokeWidth = stroke)
                            // Trattino orizzontale verso il contenuto.
                            drawLine(lineColor, Offset(x, midY), Offset(size.width, midY), strokeWidth = stroke)
                        } else {
                            // Antenato: verticale continua.
                            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
                        }
                    },
            )
        }
        content()
    }
}
