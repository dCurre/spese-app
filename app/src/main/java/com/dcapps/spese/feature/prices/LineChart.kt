package com.dcapps.spese.feature.prices

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import com.dcapps.spese.core.model.PriceHistoryPoint
import com.dcapps.spese.ui.theme.categoricalColor
import kotlin.math.abs

/**
 * Grafico a linee dell'andamento del prezzo unitario nel tempo. Mostra la scala sull'asse Y
 * (min/max in €/unità), le date (primo/ultimo periodo) sull'asse X, e al tocco di un punto un
 * riepilogo (periodo · prezzo medio) sopra il grafico, così è chiaro su quali dati è costruito.
 */
@Composable
fun LineChart(
    points: List<PriceHistoryPoint>,
    unitSymbol: String,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) {
        Box(modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.chart_insufficient_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val values = points.map { it.avgUnitPrice.toFloat() }
    val lineColor = categoricalColor(0)
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val min = values.min()
    val max = values.max()
    val range = (max - min).takeIf { it > 0f } ?: 1f

    // Punto selezionato col tocco (-1 = nessuno).
    var selected by remember(points) { mutableIntStateOf(-1) }

    Column(modifier.fillMaxWidth()) {
        // Riepilogo del punto selezionato (o dell'ultimo), così si capiscono i dati mostrati.
        val shown = if (selected in points.indices) selected else points.lastIndex
        val p = points[shown]
        Text(
            text = stringResource(
                R.string.chart_point_tooltip,
                p.period,
                "%.2f".format(p.avgUnitPrice),
                unitSymbol,
            ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected in points.indices) lineColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Row(Modifier.fillMaxWidth()) {
            // Asse Y: etichette max (in alto) e min (in basso).
            Column(
                modifier = Modifier.height(160.dp).padding(end = 6.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                Text("%.2f".format(max), style = MaterialTheme.typography.labelSmall, color = axisColor)
                Text("%.2f".format(min), style = MaterialTheme.typography.labelSmall, color = axisColor)
            }

            Canvas(
                Modifier.weight(1f).height(160.dp).pointerInput(points) {
                    detectTapGestures { tap ->
                        // Trova il punto col marker più vicino al tocco (asse X).
                        val padding = 12.dp.toPx()
                        val w = size.width - padding * 2
                        val stepX = if (points.size > 1) w / (points.size - 1) else 0f
                        var best = 0
                        var bestDist = Float.MAX_VALUE
                        points.indices.forEach { i ->
                            val x = padding + stepX * i
                            val d = abs(tap.x - x)
                            if (d < bestDist) { bestDist = d; best = i }
                        }
                        selected = if (selected == best) -1 else best
                    }
                },
            ) {
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

                // Linea di base.
                drawLine(
                    color = axisColor.copy(alpha = 0.3f),
                    start = Offset(padding, padding + h),
                    end = Offset(padding + w, padding + h),
                    strokeWidth = 1.dp.toPx(),
                )

                val path = Path()
                values.indices.forEach { i ->
                    val pt = pointFor(i)
                    if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx()))

                values.indices.forEach { i ->
                    val isSel = i == selected
                    drawCircle(
                        color = lineColor,
                        radius = (if (isSel) 6.dp else 4.dp).toPx(),
                        center = pointFor(i),
                    )
                }
            }
        }

        // Asse X: primo e ultimo periodo.
        Row(
            Modifier.fillMaxWidth().padding(start = 36.dp, top = 4.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text(points.first().period, style = MaterialTheme.typography.labelSmall, color = axisColor)
            Text(points.last().period, style = MaterialTheme.typography.labelSmall, color = axisColor)
        }
    }
}
