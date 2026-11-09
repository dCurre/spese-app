package com.dcapps.spese.feature.prices

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.dcapps.spese.core.model.Product
import com.dcapps.spese.navigation.OnCreateRequested
import com.dcapps.spese.navigation.Routes
import com.dcapps.spese.ui.components.ConfirmDialog
import com.dcapps.spese.ui.components.ErrorSnackbarEffect
import com.dcapps.spese.ui.components.ErrorState
import com.dcapps.spese.ui.components.RefreshOnResume
import com.dcapps.spese.ui.components.RowDivider
import com.dcapps.spese.ui.components.SwipeToDeleteRow
import com.dcapps.spese.ui.components.IconCount
import com.dcapps.spese.ui.components.SortBar
import com.dcapps.spese.ui.components.SortOption
import com.dcapps.spese.ui.components.SpeseSearchField
import com.dcapps.spese.ui.components.SelectableRow
import com.dcapps.spese.ui.components.SelectionTopBar
import com.dcapps.spese.ui.components.SpeseTopAppBar
import com.dcapps.spese.ui.theme.SpeseTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PriceListScreen(
    onOpenProduct: (Product) -> Unit,
    viewModel: PriceListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sel by viewModel.selection.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showDeleteSelected by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    ErrorSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
    )

    OnCreateRequested(Routes.PRICES) {
        showCreate = true
    }

    // In modalità selezione, il tasto Indietro annulla la selezione invece di uscire.
    BackHandler(enabled = sel.selectionMode) { viewModel.selection.clear() }

    // Rinfresca al ritorno dal dettaglio prodotto (es. nuovi acquisti → conteggio/minimo prezzo).
    RefreshOnResume { viewModel.load() }

    Scaffold(
        topBar = {
            if (sel.selectionMode) {
                SelectionTopBar(
                    count = sel.selectedIds.size,
                    onCancel = { viewModel.selection.clear() },
                    onDelete = { showDeleteSelected = true },
                )
            } else {
                SpeseTopAppBar(title = stringResource(R.string.prices_title))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.load(showLoading = true) },
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            when {
                state.error != null -> ErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.load(showLoading = true) },
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                ) {
                    item {
                        SpeseSearchField(
                            query = state.query,
                            onQueryChange = viewModel::onQueryChange,
                            placeholder = stringResource(R.string.prices_search),
                        )
                    }
                    item {
                        // Ordinamento + bottone filtro marca affiancati.
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SortBar(
                                options = listOf(
                                    SortOption(ProductSort.NAME, stringResource(R.string.sort_name)),
                                    SortOption(ProductSort.BRAND, stringResource(R.string.sort_brand)),
                                ),
                                selectedKey = state.sort,
                                ascending = state.sortAsc,
                                onSelect = { viewModel.onSortChange(it) },
                                modifier = Modifier.weight(1f),
                            )
                            if (state.availableBrands.isNotEmpty()) {
                                BrandFilterButton(
                                    brands = state.availableBrands,
                                    selected = state.brandFilter,
                                    onApply = { viewModel.setBrandFilter(it) },
                                )
                            }
                        }
                    }
                    if (state.visibleProducts.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.prices_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    items(state.visibleProducts, key = { it.id }) { product ->
                        val selected = product.id in sel.selectedIds
                        // Contenuto riga: condiviso tra modalità normale e selezione.
                        val rowContent: @Composable () -> Unit = {
                            SelectableRow(
                                selectionMode = sel.selectionMode,
                                selected = selected,
                                onClick = { onOpenProduct(product) },
                                onLongClick = { viewModel.selection.start(product.id) },
                                onToggle = { viewModel.selection.toggle(product.id) },
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    // Sottotitolo: marca · unità · (icona scontrino + n° acquisti).
                                    Row(
                                        modifier = Modifier.padding(top = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        val subtitle = listOfNotNull(
                                            product.brand?.takeIf { it.isNotBlank() },
                                            product.defaultUnitSymbol?.let { stringResource(R.string.prices_unit, it) },
                                        ).joinToString(" · ")
                                        if (subtitle.isNotBlank()) {
                                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconCount(
                                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                            text = "${product.purchaseCount}",
                                            contentDescription = stringResource(R.string.prices_count_cd),
                                        )
                                    }
                                }
                                // Minimo storico a destra, centrato in altezza: "min" piccolo sopra,
                                // valore con unità (prezzo al litro/ml/…) sotto.
                                product.minPrice?.let { min ->
                                    val unit = product.defaultUnitSymbol ?: "u"
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.padding(end = 8.dp),
                                    ) {
                                        Text(
                                            stringResource(R.string.prices_min_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            stringResource(R.string.prices_min_value, min, unit),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SpeseTheme.colors.credit,
                                        )
                                    }
                                }
                                if (!sel.selectionMode) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        // In selezione niente swipe-to-delete (si elimina dalla barra contestuale).
                        if (sel.selectionMode) {
                            rowContent()
                        } else {
                            SwipeToDeleteRow(onRequestDelete = { productToDelete = product }) { rowContent() }
                        }
                        RowDivider()
                    }
                }
            }
        }
    }

    if (showCreate) {
        NewProductDialog(
            units = state.units,
            onDismiss = { showCreate = false },
            onConfirm = { name, brand, unitId ->
                viewModel.createProduct(name, brand, unitId)
                showCreate = false
            },
        )
    }

    productToDelete?.let { product ->
        ConfirmDialog(
            title = stringResource(R.string.product_delete_title),
            message = stringResource(R.string.product_delete_message, product.name),
            onConfirm = { viewModel.deleteProduct(product) },
            onDismiss = { productToDelete = null },
        )
    }

    // Conferma eliminazione multipla (issue #10).
    if (showDeleteSelected) {
        ConfirmDialog(
            title = stringResource(R.string.products_delete_selected_title),
            message = stringResource(R.string.products_delete_selected_message, sel.selectedIds.size),
            onConfirm = { viewModel.deleteSelected() },
            onDismiss = { showDeleteSelected = false },
        )
    }
}

/**
 * Bottone con icona filtro che apre una dropdown di marche a selezione multipla. Le modifiche
 * si applicano solo al "Conferma"; "Pulisci filtri" azzera la selezione. Un pallino sull'icona
 * segnala quando c'è almeno un filtro attivo.
 */
@Composable
private fun BrandFilterButton(
    brands: List<String>,
    selected: Set<String>,
    onApply: (Set<String>) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    // Selezione temporanea mentre il menu è aperto (confermata solo su "Conferma").
    var temp by remember(expanded) { mutableStateOf(selected) }

    Box {
        IconButton(onClick = { expanded = true }) {
            BadgedBox(
                badge = {
                    if (selected.isNotEmpty()) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ) { Text("${selected.size}") }
                    }
                },
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = stringResource(R.string.prices_filter_brand),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Text(
                stringResource(R.string.prices_filter_brand),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            brands.forEach { brand ->
                DropdownMenuItem(
                    text = { Text(brand) },
                    leadingIcon = {
                        Checkbox(
                            checked = brand in temp,
                            onCheckedChange = null,
                        )
                    },
                    onClick = {
                        temp = if (brand in temp) temp - brand else temp + brand
                    },
                )
            }
            RowDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // "Pulisci filtri" azzera e applica subito (chiude il menu), senza richiedere Conferma.
                TextButton(onClick = { onApply(emptySet()); expanded = false }) {
                    Text(stringResource(R.string.filters_clear))
                }
                TextButton(onClick = { onApply(temp); expanded = false }) {
                    Text(stringResource(R.string.action_confirm))
                }
            }
        }
    }
}
