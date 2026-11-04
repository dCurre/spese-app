package com.dcapps.spese.feature.expenses

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dcapps.spese.core.model.BalanceEntry
import com.dcapps.spese.core.model.BalanceTotal
import com.dcapps.spese.core.util.toEuro
import com.dcapps.spese.ui.theme.categoricalColor
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Pannello saldo: grafico a ciambella "chi ha pagato quanto" (dai totali) + elenco
 * "chi deve a chi" (dalle righe di balance con toPay > 0).
 *
 * L'identità nel grafico è rafforzata da etichette dirette (nome + importo in legenda),
 * mai affidata al solo colore.
 */
@Composable
fun BalanceSection(
    totals: List<BalanceTotal>,
    balance: List<BalanceEntry>,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Saldo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            if (totals.isEmpty()) {
                Text(
                    "Nessun dato di saldo.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                return@Column
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DoughnutChart(
                    values = totals.map { it.amount },
                    modifier = Modifier.size(120.dp),
                )
                Spacer(Modifier.width(16.dp))
                Legend(totals)
            }

            // "Chi deve a chi": solo le direzioni con debito reale (toPay > 0).
            val debts = balance.filter { it.toPay > 0.0 }
            if (debts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Chi deve a chi", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                debts.forEach { d ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${d.buyer} → ${d.receiver}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            d.toPay.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toEuro(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoughnutChart(values: List<Double>, modifier: Modifier = Modifier) {
    val total = values.sumOf { abs(it) }.takeIf { it > 0 } ?: 1.0
    val surface = MaterialTheme.colorScheme.surface
    Canvas(modifier) {
        val stroke = size.minDimension * 0.18f
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        var startAngle = -90f
        values.forEachIndexed { index, value ->
            val sweep = (abs(value) / total * 360f).toFloat()
            // Gap di 2px sul colore di superficie tra i segmenti.
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

@Composable
private fun Legend(totals: List<BalanceTotal>) {
    Column {
        totals.forEachIndexed { index, t ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(categoricalColor(index)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${t.name} · ${BigDecimal(t.amount).setScale(2, RoundingMode.HALF_UP).toEuro()}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun Double.toBigDecimal(): BigDecimal = BigDecimal(this)
