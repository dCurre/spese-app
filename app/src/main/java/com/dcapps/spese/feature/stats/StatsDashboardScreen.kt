package com.dcapps.spese.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.R
import com.dcapps.spese.core.model.ExpenseStats
import com.dcapps.spese.core.model.PeriodBucket
import com.dcapps.spese.core.util.toEuro
import com.dcapps.spese.ui.components.DoughnutChartWithLegend
import com.dcapps.spese.ui.components.DoughnutSlice
import com.dcapps.spese.ui.components.SpeseTopAppBar
import com.dcapps.spese.ui.theme.SpeseTheme
import com.dcapps.spese.ui.theme.categoricalColor
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Dashboard statistiche SPESE personali: totale/variazione, andamento nel tempo (barre) e
 * ripartizione per tipologia (ciambella). Aggregazione lato backend (online-only).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsDashboardScreen(
    onBack: () -> Unit,
    viewModel: StatsDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SpeseTopAppBar(
                title = stringResource(R.string.stats_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Selettore periodo.
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val periods = listOf(
                    StatsPeriod.THIS_MONTH to R.string.stats_period_month,
                    StatsPeriod.LAST_6_MONTHS to R.string.stats_period_6months,
                    StatsPeriod.THIS_YEAR to R.string.stats_period_year,
                )
                periods.forEachIndexed { index, (period, labelRes) ->
                    SegmentedButton(
                        selected = state.period == period,
                        onClick = { viewModel.selectPeriod(period) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                    ) { Text(stringResource(labelRes)) }
                }
            }

            when {
                state.isLoading -> Box(
                    Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(Modifier.size(32.dp)) }

                state.error != null -> ErrorBox(message = state.error!!, onRetry = viewModel::retry)

                state.stats != null -> StatsContent(state.stats!!)
            }
        }
    }
}

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

@Composable
private fun StatsContent(stats: ExpenseStats) {
    // Card totale + variazione vs periodo precedente.
    TotalCard(stats)

    if (stats.count == 0) {
        Text(
            stringResource(R.string.stats_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        return
    }

    // Andamento nel tempo (barre).
    SectionCard(title = stringResource(R.string.stats_over_time)) {
        BarChart(stats.byPeriod)
    }

    // Ripartizione per tipologia (ciambella).
    val otherLabel = stringResource(R.string.stats_type_other)
    val slices = stats.byType.map {
        DoughnutSlice(
            label = it.name ?: otherLabel,
            amount = it.total.toDouble(),
            formattedAmount = it.total.setScale(2, RoundingMode.HALF_UP).toEuro(),
        )
    }
    if (slices.isNotEmpty()) {
        SectionCard(title = stringResource(R.string.stats_by_type)) {
            DoughnutChartWithLegend(slices = slices)
        }
    }
}

@Composable
private fun TotalCard(stats: ExpenseStats) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = primary.copy(alpha = 0.14f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.stats_total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stats.total.setScale(2, RoundingMode.HALF_UP).toEuro(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.stats_count, stats.count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Variazione percentuale vs periodo precedente (se calcolabile).
            val prev = stats.previousTotal
            if (prev > BigDecimal.ZERO) {
                val delta = stats.total.subtract(prev)
                val pct = delta.divide(prev, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)
                val up = delta.signum() >= 0
                val color = if (up) SpeseTheme.colors.debit else SpeseTheme.colors.credit
                val sign = if (up) "+" else ""
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.stats_change_vs_prev, "$sign$pct%"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Grafico a barre verticali: una barra per periodo, altezza proporzionale al totale. */
@Composable
private fun BarChart(buckets: List<PeriodBucket>) {
    if (buckets.isEmpty()) {
        Text(
            stringResource(R.string.chart_insufficient_data),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val values = buckets.map { it.total.toDouble() }
    val max = values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
    val barColor = categoricalColor(0)
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            buckets.forEachIndexed { i, _ ->
                val frac = (values[i] / max).toFloat().coerceIn(0f, 1f)
                // Barra: contenitore alto quanto la riga, la barra riempie la frazione dal basso.
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Canvas(Modifier.fillMaxWidth().fillMaxHeight(frac.coerceAtLeast(0.01f))) {
                        drawRect(color = barColor, size = size)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        // Etichette X: primo, centrale, ultimo periodo (evita sovrapposizioni con molti bucket).
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(buckets.first().period, style = MaterialTheme.typography.labelSmall, color = axisColor)
            if (buckets.size >= 3) {
                Text(buckets[buckets.size / 2].period, style = MaterialTheme.typography.labelSmall, color = axisColor)
            }
            Text(buckets.last().period, style = MaterialTheme.typography.labelSmall, color = axisColor)
        }
    }
}
