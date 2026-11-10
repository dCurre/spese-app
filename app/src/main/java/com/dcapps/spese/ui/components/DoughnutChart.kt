package com.dcapps.spese.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dcapps.spese.ui.theme.categoricalColor
import kotlin.math.abs

/** Una fetta del grafico: etichetta diretta + importo (già formattato per la legenda). */
data class DoughnutSlice(val label: String, val amount: Double, val formattedAmount: String)

/**
 * Grafico a ciambella generico con legenda a etichette dirette (mai solo colore). Condiviso tra il
 * pannello saldo delle spese e la dashboard statistiche. L'ordine delle fette determina il colore
 * categorico ([categoricalColor]), coerente tra ciambella e legenda.
 */
@Composable
fun DoughnutChartWithLegend(
    slices: List<DoughnutSlice>,
    modifier: Modifier = Modifier,
    chartSize: androidx.compose.ui.unit.Dp = 120.dp,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        DoughnutChart(values = slices.map { it.amount }, modifier = Modifier.size(chartSize))
        Spacer(Modifier.width(16.dp))
        Column {
            slices.forEachIndexed { index, s ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    Box(
                        Modifier.size(12.dp).clip(CircleShape).background(categoricalColor(index)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${s.label} · ${s.formattedAmount}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/** Solo la ciambella (senza legenda). Segmenti colorati con [categoricalColor] per indice. */
@Composable
fun DoughnutChart(values: List<Double>, modifier: Modifier = Modifier) {
    val total = values.sumOf { abs(it) }.takeIf { it > 0 } ?: 1.0
    Canvas(modifier) {
        val stroke = size.minDimension * 0.18f
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        var startAngle = -90f
        values.forEachIndexed { index, value ->
            val sweep = (abs(value) / total * 360f).toFloat()
            drawArc(
                color = categoricalColor(index),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            startAngle += sweep
        }
    }
}
