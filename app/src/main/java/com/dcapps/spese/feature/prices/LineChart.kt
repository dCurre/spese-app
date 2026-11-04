package com.dcapps.spese.feature.prices

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dcapps.spese.ui.theme.categoricalColor

/**
 * Grafico a linee semplice per l'andamento dei prezzi: una serie di punti (prezzo medio) nel
 * tempo. Linea 2px, marker ≥8px, nessun asse invadente (solo la linea di base).
 */
@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    if (values.size < 2) {
        Box(modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            Text(
                "Dati insufficienti per il grafico",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val lineColor = categoricalColor(0)
    val min = values.min()
    val max = values.max()
    val range = (max - min).takeIf { it > 0f } ?: 1f

    Canvas(modifier.fillMaxWidth().height(160.dp)) {
        val padding = 12.dp.toPx()
        val w = size.width - padding * 2
        val h = size.height - padding * 2
        val stepX = w / (values.size - 1)

        fun pointFor(index: Int): Offset {
            val x = padding + stepX * index
            val norm = (values[index] - min) / range
            val y = padding + h - norm * h
            return Offset(x, y)
        }

        val path = Path()
        values.indices.forEach { i ->
            val p = pointFor(i)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx()))

        values.indices.forEach { i ->
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = pointFor(i))
        }
    }
}
