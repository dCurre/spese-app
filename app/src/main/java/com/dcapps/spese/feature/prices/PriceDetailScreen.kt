package com.dcapps.spese.feature.prices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.R
import com.dcapps.spese.ui.components.ConfirmDialog
import com.dcapps.spese.ui.components.ErrorSnackbarEffect
import com.dcapps.spese.ui.components.OverflowMenu
import com.dcapps.spese.ui.components.RowDivider
import com.dcapps.spese.ui.components.SpeseFilterChip
import com.dcapps.spese.ui.components.SpeseTopAppBar
import com.dcapps.spese.ui.theme.SpeseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceDetailScreen(
    onBack: () -> Unit,
    viewModel: PriceDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddPurchase by remember { mutableStateOf(false) }
    var editingPurchase by remember { mutableStateOf<com.dcapps.spese.core.model.Purchase?>(null) }
    var purchaseToDelete by remember { mutableStateOf<com.dcapps.spese.core.model.Purchase?>(null) }
    var showEditProduct by remember { mutableStateOf(false) }
    var showDeleteProduct by remember { mutableStateOf(false) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    ErrorSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
    )

    // Torna indietro quando il prodotto viene eliminato.
    androidx.compose.runtime.LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            SpeseTopAppBar(
                title = state.product?.name ?: state.placeholderName
                    ?: stringResource(R.string.product_detail_fallback),
                onTitleEdit = { showEditProduct = true },
                titleEditLabel = stringResource(R.string.product_edit_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    OverflowMenu { dismiss ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(R.string.product_delete_title)) },
                            onClick = { dismiss(); showDeleteProduct = true },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddPurchase = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.purchase_new_title))
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, Modifier.align(Alignment.Center).padding(32.dp))
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Statistiche prodotto (calcolate dagli acquisti: prezzo unitario).
                    item {
                        val statsUnit = state.product?.defaultUnitSymbol
                            ?: state.purchases.firstNotNullOfOrNull { it.unitSymbol }
                            ?: "u"
                        ProductStatsCard(state.purchases, statsUnit)
                    }

                    // Grafico prezzi.
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.product_price_trend), style = MaterialTheme.typography.titleMedium)
                                Row(
                                    Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Granularity.entries.forEach { g ->
                                        SpeseFilterChip(
                                            selected = state.granularity == g,
                                            onClick = { viewModel.setGranularity(g) },
                                            label = stringResource(granularityLabelRes(g)),
                                        )
                                    }
                                    // Loading locale del grafico: non fa sparire la schermata.
                                    if (state.historyLoading) {
                                        CircularProgressIndicator(
                                            Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                                LineChart(
                                    points = state.history,
                                    unitSymbol = state.product?.defaultUnitSymbol
                                        ?: state.purchases.firstNotNullOfOrNull { it.unitSymbol }
                                        ?: "u",
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            stringResource(R.string.product_purchases),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    if (state.purchases.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.product_no_purchases),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    items(state.purchases, key = { it.id }) { p ->
                        com.dcapps.spese.ui.components.SwipeToDeleteRow(onRequestDelete = { purchaseToDelete = p }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .clickable { editingPurchase = p }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "%.2f € · %.3g %s".format(p.price, p.quantity, p.unitSymbol ?: ""),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    val sub = listOfNotNull(p.purchasedAt, p.store).joinToString(" · ")
                                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val unitSym = p.unitSymbol ?: state.product?.defaultUnitSymbol ?: "u"
                                Text(
                                    "%.2f €/%s".format(p.unitPrice, unitSym),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SpeseTheme.colors.credit,
                                )
                            }
                        }
                        RowDivider()
                    }
                }
            }
        }
    }

    val storeSuggestions = state.purchases.mapNotNull { it.store?.takeIf { s -> s.isNotBlank() } }.distinct()

    if (showAddPurchase) {
        NewPurchaseDialog(
            productUnitId = state.product?.defaultUnitId,
            productUnitSymbol = state.product?.defaultUnitSymbol,
            storeSuggestions = storeSuggestions,
            onDismiss = { showAddPurchase = false },
            onConfirm = { price, qty, unitId, date, store, notes ->
                viewModel.addPurchase(price, qty, unitId, date, store, notes)
                showAddPurchase = false
            },
        )
    }

    editingPurchase?.let { p ->
        NewPurchaseDialog(
            productUnitId = state.product?.defaultUnitId,
            productUnitSymbol = state.product?.defaultUnitSymbol,
            purchase = p,
            storeSuggestions = storeSuggestions,
            onDismiss = { editingPurchase = null },
            onDelete = { editingPurchase = null; purchaseToDelete = p },
            onConfirm = { price, qty, unitId, date, store, notes ->
                viewModel.updatePurchase(p.id, price, qty, unitId, date, store, notes)
                editingPurchase = null
            },
        )
    }

    purchaseToDelete?.let { p ->
        ConfirmDialog(
            title = stringResource(R.string.product_delete_purchase),
            message = stringResource(R.string.product_delete_purchase_message),
            onConfirm = { viewModel.deletePurchase(p) },
            onDismiss = { purchaseToDelete = null },
        )
    }

    if (showEditProduct) {
        state.product?.let { p ->
            NewProductDialog(
                units = state.units,
                product = p,
                onDismiss = { showEditProduct = false },
                onConfirm = { name, brand, unitId ->
                    viewModel.updateProduct(name, brand, unitId)
                    showEditProduct = false
                },
            )
        }
    }

    if (showDeleteProduct) {
        ConfirmDialog(
            title = stringResource(R.string.product_delete_title),
            message = stringResource(R.string.product_delete_message, state.product?.name ?: ""),
            onConfirm = { viewModel.deleteProduct() },
            onDismiss = { showDeleteProduct = false },
        )
    }
}

@androidx.compose.runtime.Composable
private fun ProductStatsCard(
    purchases: List<com.dcapps.spese.core.model.Purchase>,
    unitSymbol: String,
) {
    if (purchases.isEmpty()) return
    // Prezzo unitario per confronti (prezzo/quantità, già calcolato dal backend come unit_price).
    val byDate = purchases.sortedBy { it.purchasedAt }
    val minP = purchases.minByOrNull { it.unitPrice }
    val last = byDate.lastOrNull()
    val lastPrice = last?.unitPrice ?: 0.0
    val minPrice = minP?.unitPrice ?: 0.0
    val variationPct = if (minPrice > 0) ((lastPrice - minPrice) / minPrice * 100) else 0.0

    // Niente card: titolo "Statistiche" evidente + righe su sfondo, coerente con la sezione acquisti.
    androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxWidth()) {
        androidx.compose.material3.Text(
            stringResource(R.string.profile_stats),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = androidx.compose.ui.Modifier.padding(bottom = 4.dp),
        )
        StatLine(stringResource(R.string.product_stats_min_price), "%.2f €/%s".format(minPrice, unitSymbol) + (minP?.store?.let { " · $it" } ?: ""))
        StatLine(stringResource(R.string.product_stats_last_price), "%.2f €/%s".format(lastPrice, unitSymbol))
        StatLine(
            stringResource(R.string.product_stats_variation),
            if (variationPct <= 0.0) stringResource(R.string.product_stats_at_minimum) else "+%.0f%%".format(variationPct),
        )
    }
}

@androidx.annotation.StringRes
private fun granularityLabelRes(g: Granularity): Int = when (g) {
    Granularity.DAY -> R.string.granularity_day
    Granularity.MONTH -> R.string.granularity_month
    Granularity.YEAR -> R.string.granularity_year
}

@androidx.compose.runtime.Composable
private fun StatLine(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        androidx.compose.ui.Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        androidx.compose.material3.Text(label, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Text(
            value,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
    }
}
