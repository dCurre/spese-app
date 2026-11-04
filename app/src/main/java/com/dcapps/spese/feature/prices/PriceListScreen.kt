package com.dcapps.spese.feature.prices

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.core.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceListScreen(
    onOpenProduct: (Product) -> Unit,
    viewModel: PriceListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Storico prezzi") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuovo prodotto")
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!)
                    TextButton(onClick = viewModel::load) { Text("Riprova") }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Cerca prodotto") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                        )
                    }
                    if (state.visibleProducts.isEmpty()) {
                        item {
                            Text(
                                "Nessun prodotto. Creane uno con il +.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                        }
                    }
                    items(state.visibleProducts, key = { it.id }) { product ->
                        Card(onClick = { onOpenProduct(product) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(product.name, style = MaterialTheme.typography.titleMedium)
                                val subtitle = listOfNotNull(
                                    product.brand?.takeIf { it.isNotBlank() },
                                    product.defaultUnitSymbol?.let { "unità: $it" },
                                ).joinToString(" · ")
                                if (subtitle.isNotBlank()) {
                                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        NewProductDialog(
            units = state.units,
            onDismiss = { showCreate = false },
            onCreate = { name, brand, unitId ->
                viewModel.createProduct(name, brand, unitId)
                showCreate = false
            },
        )
    }
}
