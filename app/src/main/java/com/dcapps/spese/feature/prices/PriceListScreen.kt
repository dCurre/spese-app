package com.dcapps.spese.feature.prices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.dcapps.spese.ui.components.ErrorSnackbarEffect
import com.dcapps.spese.ui.components.ErrorState
import com.dcapps.spese.ui.components.RowDivider
import com.dcapps.spese.ui.components.SortBar
import com.dcapps.spese.ui.components.SortOption
import com.dcapps.spese.ui.components.SpeseFilterChip
import com.dcapps.spese.ui.components.SpeseSearchField
import com.dcapps.spese.ui.components.SpeseTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceListScreen(
    onOpenProduct: (Product) -> Unit,
    viewModel: PriceListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    ErrorSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
    )

    com.dcapps.spese.navigation.OnCreateRequested(com.dcapps.spese.navigation.Routes.PRICES) {
        showCreate = true
    }

    Scaffold(
        topBar = { SpeseTopAppBar(title = stringResource(R.string.prices_title)) },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
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
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 88.dp),
                ) {
                    item {
                        SpeseSearchField(
                            query = state.query,
                            onQueryChange = viewModel::onQueryChange,
                            placeholder = stringResource(R.string.prices_search),
                        )
                    }
                    item {
                        SortBar(
                            options = listOf(
                                SortOption(ProductSort.NAME, stringResource(R.string.sort_name)),
                                SortOption(ProductSort.BRAND, stringResource(R.string.sort_brand)),
                            ),
                            selectedKey = state.sort,
                            ascending = state.sortAsc,
                            onSelect = { viewModel.onSortChange(it) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    // Filtro per marca (solo se ci sono marche): "Tutte" + un chip per marca.
                    if (state.availableBrands.isNotEmpty()) {
                        item {
                            androidx.compose.foundation.layout.Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                            ) {
                                SpeseFilterChip(
                                    selected = state.brandFilter == null,
                                    onClick = { viewModel.onBrandFilterChange(null) },
                                    label = stringResource(R.string.prices_brand_all),
                                )
                                state.availableBrands.forEach { brand ->
                                    SpeseFilterChip(
                                        selected = state.brandFilter == brand,
                                        onClick = { viewModel.onBrandFilterChange(brand) },
                                        label = brand,
                                    )
                                }
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenProduct(product) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                val subtitle = listOfNotNull(
                                    product.brand?.takeIf { it.isNotBlank() },
                                    product.defaultUnitSymbol?.let { stringResource(R.string.prices_unit, it) },
                                ).joinToString(" · ")
                                if (subtitle.isNotBlank()) {
                                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
}
