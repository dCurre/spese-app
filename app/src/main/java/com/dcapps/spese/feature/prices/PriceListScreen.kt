package com.dcapps.spese.feature.prices

import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceListScreen(
    onOpenProduct: (Product) -> Unit,
    viewModel: PriceListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    com.dcapps.spese.ui.components.ErrorSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
    )

    com.dcapps.spese.navigation.OnCreateRequested(com.dcapps.spese.navigation.Routes.PRICES) {
        showCreate = true
    }

    Scaffold(
        topBar = { com.dcapps.spese.ui.components.SpeseTopAppBar(title = stringResource(R.string.prices_title)) },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::load,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            when {
                state.error != null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!)
                    TextButton(onClick = viewModel::load) { Text(stringResource(R.string.action_retry)) }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 88.dp),
                ) {
                    item {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            placeholder = { Text(stringResource(R.string.prices_search)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            singleLine = true,
                        )
                    }
                    item {
                        com.dcapps.spese.ui.components.SortBar(
                            options = listOf(
                                com.dcapps.spese.ui.components.SortOption(Unit, stringResource(R.string.sort_name)),
                            ),
                            selectedKey = Unit,
                            ascending = state.sortAsc,
                            onSelect = { viewModel.toggleSort() },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
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
                        androidx.compose.material3.HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = com.dcapps.spese.ui.theme.SpeseTheme.colors.divider,
                        )
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
