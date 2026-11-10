package com.dcapps.spese.feature.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import com.dcapps.spese.core.model.BalanceEntry
import com.dcapps.spese.core.model.BalanceTotal
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.util.toEuro
import com.dcapps.spese.ui.components.DoughnutChartWithLegend
import com.dcapps.spese.ui.components.DoughnutSlice
import com.dcapps.spese.ui.theme.SpeseTheme
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pannello saldo: grafico a ciambella "chi ha pagato quanto" (dai totali) + elenco
 * "chi deve a chi" (dalle righe di balance con toPay > 0).
 *
 * L'identità nel grafico è rafforzata da etichette dirette (nome + importo in legenda),
 * mai affidata al solo colore.
 */
/** Raggruppamento del grafico: per persona (chi ha pagato) o per tipologia di spesa. */
private enum class GroupBy { PERSON, TYPE }

/** Voce del grafico: etichetta + importo. */
private data class Slice(val label: String, val amount: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceSection(
    totals: List<BalanceTotal>,
    balance: List<BalanceEntry>,
    expenses: List<Expense>,
    modifier: Modifier = Modifier,
) {
    var groupBy by remember { mutableStateOf(GroupBy.PERSON) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, SpeseTheme.colors.divider),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.balance_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            if (totals.isEmpty()) {
                Text(
                    stringResource(R.string.balance_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                return@Column
            }

            // Toggle Persona / Tipo (come nel frontend).
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = groupBy == GroupBy.PERSON,
                    onClick = { groupBy = GroupBy.PERSON },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.balance_by_person)) }
                SegmentedButton(
                    selected = groupBy == GroupBy.TYPE,
                    onClick = { groupBy = GroupBy.TYPE },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.balance_by_type)) }
            }

            // Dati del grafico in base al raggruppamento scelto.
            val otherTypeLabel = stringResource(R.string.balance_type_other)
            val slices: List<Slice> = when (groupBy) {
                GroupBy.PERSON -> totals.map { Slice(it.name, it.amount) }
                GroupBy.TYPE -> expenses
                    .groupBy { it.expenseType ?: otherTypeLabel }
                    .map { (type, list) -> Slice(type, list.sumOf { e -> e.amount.toDouble() }) }
                    .sortedBy { it.label.lowercase() }
            }

            Spacer(Modifier.height(12.dp))
            DoughnutChartWithLegend(
                slices = slices.map {
                    DoughnutSlice(
                        label = it.label,
                        amount = it.amount,
                        formattedAmount = BigDecimal(it.amount).setScale(2, RoundingMode.HALF_UP).toEuro(),
                    )
                },
            )

            // "Chi deve a chi": solo le direzioni con debito reale (toPay > 0).
            val debts = balance.filter { it.toPay > 0.0 }
            if (debts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.balance_who_owes), style = MaterialTheme.typography.labelLarge)
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
                            color = SpeseTheme.colors.debit,
                        )
                    }
                }
            }
        }
    }
}

private fun Double.toBigDecimal(): BigDecimal = BigDecimal(this)
