package com.dcapps.spese.feature.checklist

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.core.model.ShoppingList
import com.dcapps.spese.feature.expenses.CreateListDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    onOpenList: (ShoppingList) -> Unit,
    viewModel: ChecklistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Le mie checklist") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuova checklist")
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.error!!)
                    TextButton(onClick = viewModel::load) { Text("Riprova") }
                }
                else -> ChecklistContent(state, viewModel, onOpenList)
            }
        }
    }

    if (showCreate) {
        CreateListDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, shared ->
                viewModel.create(name, shared)
                showCreate = false
            },
        )
    }
}

@Composable
private fun ChecklistContent(
    state: ChecklistUiState,
    viewModel: ChecklistViewModel,
    onOpenList: (ShoppingList) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cerca lista") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
        }

        if (state.starred.isEmpty() && state.active.isEmpty() && state.completed.isEmpty()) {
            item {
                Text(
                    "Nessuna lista. Creane una con il +.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }

        section("Preferite", state.starred, viewModel, onOpenList)
        section("Attive", state.active, viewModel, onOpenList)
        section("Completate", state.completed, viewModel, onOpenList)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    lists: List<ShoppingList>,
    viewModel: ChecklistViewModel,
    onOpenList: (ShoppingList) -> Unit,
) {
    if (lists.isEmpty()) return
    item(key = "header-$title") {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
    items(lists, key = { it.id }) { list ->
        ChecklistCard(list, onOpenList, onStar = { viewModel.toggleStarred(list) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistCard(
    list: ShoppingList,
    onOpenList: (ShoppingList) -> Unit,
    onStar: () -> Unit,
) {
    Card(onClick = { onOpenList(list) }, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(list.name, style = MaterialTheme.typography.titleMedium)
                val total = list.itemsCount ?: 0
                val checked = list.checkedCount ?: 0
                val subtitle = buildString {
                    append(if (list.listType == "shared") "Condivisa" else "Personale")
                    if (total > 0) append(" · $checked/$total")
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onStar) {
                if (list.starred) {
                    Icon(Icons.Default.Star, contentDescription = "Rimuovi dai preferiti")
                } else {
                    Icon(Icons.Outlined.StarBorder, contentDescription = "Aggiungi ai preferiti")
                }
            }
        }
    }
}
