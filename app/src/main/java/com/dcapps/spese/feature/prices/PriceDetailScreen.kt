package com.dcapps.spese.feature.prices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceDetailScreen(
    onBack: () -> Unit,
    viewModel: PriceDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddPurchase by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.product?.name ?: "Prodotto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddPurchase = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuovo acquisto")
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, Modifier.align(Alignment.Center).padding(32.dp))
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Grafico prezzi.
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Andamento prezzo", style = MaterialTheme.typography.titleMedium)
                                Row(
                                    Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Granularity.entries.forEach { g ->
                                        FilterChip(
                                            selected = state.granularity == g,
                                            onClick = { viewModel.setGranularity(g) },
                                            label = { Text(g.label) },
                                        )
                                    }
                                }
                                LineChart(
                                    values = state.history.map { it.avgUnitPrice.toFloat() },
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            "Acquisti",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    if (state.purchases.isEmpty()) {
                        item {
                            Text(
                                "Nessun acquisto registrato.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    items(state.purchases, key = { it.id }) { p ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "%.2f € · %.3g %s".format(p.price, p.quantity, p.unitSymbol ?: ""),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    val sub = listOfNotNull(p.purchasedAt, p.store).joinToString(" · ")
                                    Text(sub, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    "%.2f €/u".format(p.unitPrice),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPurchase) {
        NewPurchaseDialog(
            defaultUnitId = state.product?.defaultUnitId,
            units = state.units,
            onDismiss = { showAddPurchase = false },
            onCreate = { price, qty, unitId, date, store ->
                viewModel.addPurchase(price, qty, unitId, date, store)
                showAddPurchase = false
            },
        )
    }
}
