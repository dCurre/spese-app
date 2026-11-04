package com.dcapps.spese.feature.admin

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Amministrazione") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                !state.isAdmin -> Text(
                    "Accesso riservato agli amministratori.",
                    Modifier.align(Alignment.Center).padding(32.dp),
                )
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> Column(Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Tipi spesa") })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Unità") })
                        Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Ruoli") })
                    }
                    when (tab) {
                        0 -> NameSection(
                            items = state.expenseTypes.map { it.id to it.name },
                            protectedIds = state.expenseTypes.filter { it.protected }.map { it.id }.toSet(),
                            onAdd = viewModel::addExpenseType,
                            onUpdate = viewModel::updateExpenseType,
                            onDelete = viewModel::deleteExpenseType,
                        )
                        1 -> UnitSection(viewModel, state)
                        2 -> NameSection(
                            items = state.roles.map { it.id to it.name },
                            protectedIds = state.roles.filter { it.protected }.map { it.id }.toSet(),
                            onAdd = viewModel::addRole,
                            onUpdate = viewModel::updateRole,
                            onDelete = viewModel::deleteRole,
                        )
                    }
                }
            }
        }
    }
}

/** Sezione generica per elementi con solo "nome" (tipi spesa, ruoli). */
@Composable
private fun NameSection(
    items: List<Pair<Long, String>>,
    protectedIds: Set<Long>,
    onAdd: (String) -> Unit,
    onUpdate: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var editing by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            TextButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Aggiungi", Modifier.padding(start = 4.dp))
            }
        }
        items(items, key = { it.first }) { (id, name) ->
            val isProtected = id in protectedIds
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (!isProtected) {
                        IconButton(onClick = { editing = id to name }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifica")
                        }
                        IconButton(onClick = { onDelete(id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina")
                        }
                    } else {
                        Text("protetto", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 16.dp))
                    }
                }
            }
        }
    }

    if (showAdd) {
        NameDialog(title = "Aggiungi", initial = "", onDismiss = { showAdd = false }, onConfirm = {
            onAdd(it); showAdd = false
        })
    }
    editing?.let { (id, name) ->
        NameDialog(title = "Modifica", initial = name, onDismiss = { editing = null }, onConfirm = {
            onUpdate(id, it); editing = null
        })
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = value.isNotBlank(), onClick = { onConfirm(value.trim()) }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun UnitSection(viewModel: AdminViewModel, state: AdminUiState) {
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            TextButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Aggiungi", Modifier.padding(start = 4.dp))
            }
        }
        items(state.units, key = { it.id }) { u ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text("${u.name} (${u.symbol})", style = MaterialTheme.typography.bodyLarge)
                        Text(u.category, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!u.protected) {
                        IconButton(onClick = { viewModel.deleteUnit(u.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina")
                        }
                    } else {
                        Text("protetto", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 16.dp))
                    }
                }
            }
        }
    }

    if (showAdd) {
        UnitDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, symbol, category ->
                viewModel.addUnit(name, symbol, category); showAdd = false
            },
        )
    }
}

@Composable
private fun UnitDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, symbol: String, category: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("peso") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova unità") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(symbol, { symbol = it }, label = { Text("Simbolo") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(category, { category = it }, label = { Text("Categoria (peso/volume/numero/lunghezza/energia)") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && symbol.isNotBlank() && category.isNotBlank(),
                onClick = { onConfirm(name.trim(), symbol.trim(), category.trim()) },
            ) { Text("Crea") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
