package com.dcapps.spese.feature.prices

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.R
import com.dcapps.spese.core.model.Purchase
import com.dcapps.spese.ui.components.ConfirmDialog
import com.dcapps.spese.ui.components.ErrorSnackbarEffect
import com.dcapps.spese.ui.components.OverflowMenu
import com.dcapps.spese.ui.components.RowDivider
import com.dcapps.spese.ui.components.SelectableRow
import com.dcapps.spese.ui.components.SelectionTopBar
import com.dcapps.spese.ui.components.SpeseFilterChip
import com.dcapps.spese.ui.components.SpeseTopAppBar
import com.dcapps.spese.ui.components.PendingSyncBadge
import com.dcapps.spese.ui.components.SwipeToDeleteRow
import com.dcapps.spese.ui.theme.SpeseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceDetailScreen(
    onBack: () -> Unit,
    viewModel: PriceDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sel by viewModel.selection.state.collectAsStateWithLifecycle()
    var showAddPurchase by remember { mutableStateOf(false) }
    var editingPurchase by remember { mutableStateOf<Purchase?>(null) }
    var purchaseToDelete by remember { mutableStateOf<Purchase?>(null) }
    var showEditProduct by remember { mutableStateOf(false) }
    var showDeleteProduct by remember { mutableStateOf(false) }
    var showDeleteSelected by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    ErrorSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
    )

    // Torna indietro quando il prodotto viene eliminato.
    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    // In modalità selezione, il tasto Indietro annulla la selezione invece di uscire.
    BackHandler(enabled = sel.selectionMode) { viewModel.selection.clear() }

    // Pannello grafico persistente (P11): parte collassato mostrando solo intestazione + chip
    // periodo; trascinandolo verso l'alto appare il grafico. Lo sfondo (statistiche + acquisti)
    // resta visibile e usabile.
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
        ),
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Colori del tema app: senza, il BottomSheetScaffold usa un containerColor Material diverso
        // (lo sfondo dello schermo "cambiava colore"). Lo sheet usa surface, il resto background.
        containerColor = MaterialTheme.colorScheme.background,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        // Altezza da collassato: giusto intestazione + chip periodo (il grafico resta nascosto).
        sheetPeekHeight = 120.dp,
        topBar = {
            if (sel.selectionMode) {
                SelectionTopBar(
                    count = sel.selectedIds.size,
                    onCancel = { viewModel.selection.clear() },
                    onDelete = { showDeleteSelected = true },
                )
            } else {
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
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.product_delete_title)) },
                                onClick = { dismiss(); showDeleteProduct = true },
                            )
                        }
                    },
                )
            }
        },
        sheetContent = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                // Intestazione + chip periodo: sempre visibili (anche da collassato).
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
                    if (state.historyLoading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
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
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, Modifier.align(Alignment.Center).padding(32.dp))
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    // innerPadding del BottomSheetScaffold copre già il peek del pannello grafico;
                    // il bottom extra serve a non far coprire l'ultima riga dal FAB in overlay.
                    contentPadding = PaddingValues(
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
                        val rowContent: @Composable () -> Unit = {
                            SelectableRow(
                                selectionMode = sel.selectionMode,
                                selected = p.id in sel.selectedIds,
                                onClick = { editingPurchase = p },
                                onLongClick = { viewModel.selection.start(p.id) },
                                onToggle = { viewModel.selection.toggle(p.id) },
                                modifier = Modifier.padding(vertical = 0.dp),
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
                                // Badge "in attesa di sync" se l'acquisto ha modifiche locali non inviate (punto 2).
                                PendingSyncBadge(
                                    show = state.showPendingSync && p.pendingSync,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                                val unitSym = p.unitSymbol ?: state.product?.defaultUnitSymbol ?: "u"
                                Text(
                                    "%.2f €/%s".format(p.unitPrice, unitSym),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SpeseTheme.colors.credit,
                                )
                            }
                        }
                        if (sel.selectionMode) {
                            rowContent()
                        } else {
                            SwipeToDeleteRow(onRequestDelete = { purchaseToDelete = p }) { rowContent() }
                        }
                        RowDivider()
                    }
                }
            }

            // FAB "aggiungi acquisto": overlay in basso a destra (BottomSheetScaffold non ha
            // uno slot FAB dedicato). Sta sopra lo sfondo, il pannello grafico gli passa davanti.
            // Nascosto durante caricamento/errore e in modalità selezione.
            if (!state.isLoading && state.error == null && !sel.selectionMode) {
                FloatingActionButton(
                    onClick = { showAddPurchase = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.purchase_new_title))
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

    // Conferma eliminazione multipla acquisti (issue #10).
    if (showDeleteSelected) {
        ConfirmDialog(
            title = stringResource(R.string.purchases_delete_selected_title),
            message = stringResource(R.string.purchases_delete_selected_message, sel.selectedIds.size),
            onConfirm = { viewModel.deleteSelected() },
            onDismiss = { showDeleteSelected = false },
        )
    }
}

@Composable
private fun ProductStatsCard(
    purchases: List<Purchase>,
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
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.profile_stats),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        StatLine(stringResource(R.string.product_stats_min_price), "%.2f €/%s".format(minPrice, unitSymbol) + (minP?.store?.let { " · $it" } ?: ""))
        StatLine(stringResource(R.string.product_stats_last_price), "%.2f €/%s".format(lastPrice, unitSymbol))
        StatLine(
            stringResource(R.string.product_stats_variation),
            if (variationPct <= 0.0) stringResource(R.string.product_stats_at_minimum) else "+%.0f%%".format(variationPct),
        )
    }
}

@StringRes
private fun granularityLabelRes(g: Granularity): Int = when (g) {
    Granularity.DAY -> R.string.granularity_day
    Granularity.MONTH -> R.string.granularity_month
    Granularity.YEAR -> R.string.granularity_year
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        // Spazio minimo garantito tra etichetta e valore (evita che si tocchino quando il valore
        // è lungo, es. "Prezzo minimo" + prezzo + negozio).
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
