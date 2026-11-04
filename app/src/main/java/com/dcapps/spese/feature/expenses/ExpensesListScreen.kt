package com.dcapps.spese.feature.expenses

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.BuildConfig
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.ui.components.ConfirmDialog
import com.dcapps.spese.ui.components.MessageSnackbarEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesListScreen(
    onOpenList: (ExpensesList) -> Unit,
    viewModel: ExpensesListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<ExpensesList?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    MessageSnackbarEffect(state.message, snackbarHostState, viewModel::clearMessage)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Le mie liste") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuova lista")
            }
        },
    ) { innerPadding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::load,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            when {
                state.isLoading && state.activeLists.isEmpty() && state.paidLists.isEmpty() ->
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                state.error != null -> ErrorState(state.error!!, onRetry = viewModel::load)
                else -> ListContent(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onTogglePaid = viewModel::togglePaidListsShown,
                    onOpenList = onOpenList,
                    onDeleteList = { listToDelete = it },
                )
            }
        }
    }

    listToDelete?.let { list ->
        ConfirmDialog(
            title = "Elimina lista",
            message = "Vuoi eliminare \"${list.name}\" e tutte le sue spese?",
            onConfirm = { viewModel.deleteList(list) },
            onDismiss = { listToDelete = null },
        )
    }

    if (showCreateDialog) {
        CreateListDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, shared ->
                viewModel.createList(name, shared)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun ListContent(
    state: ExpensesListUiState,
    onQueryChange: (String) -> Unit,
    onTogglePaid: () -> Unit,
    onOpenList: (ExpensesList) -> Unit,
    onDeleteList: (ExpensesList) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cerca lista") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
        }

        if (state.activeLists.isEmpty() && state.paidLists.isEmpty()) {
            item {
                Text(
                    "Nessuna lista. Creane una con il +.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }

        if (state.activeLists.isNotEmpty()) {
            item { SectionHeader("Attive") }
            items(state.activeLists, key = { it.id }) { list ->
                ListCard(list, onOpenList, onDeleteList)
            }
        }

        if (state.paidLists.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionHeader("Saldate")
                    TextButton(onClick = onTogglePaid) {
                        Text(if (state.paidListsShown) "Nascondi" else "Mostra")
                    }
                }
            }
            if (state.paidListsShown) {
                items(state.paidLists, key = { it.id }) { list ->
                    ListCard(list, onOpenList, onDeleteList)
                }
            }
        }

        item {
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun ListCard(
    list: ExpensesList,
    onOpenList: (ExpensesList) -> Unit,
    onDeleteList: (ExpensesList) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOpenList(list) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(list.name, style = MaterialTheme.typography.titleMedium)
                val subtitle = buildString {
                    append(if (list.type == com.dcapps.spese.core.model.ListType.SHARED) "Condivisa" else "Personale")
                    list.participantsCount?.let { append(" · $it partecipanti") }
                    list.expensesCount?.let { append(" · $it spese") }
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { onDeleteList(list) }) {
                Icon(Icons.Default.Delete, contentDescription = "Elimina")
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Text("Riprova")
        }
    }
}
