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
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dcapps.spese.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.ui.components.ConfirmDialog
import com.dcapps.spese.ui.components.ErrorSnackbarEffect

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

    ErrorSnackbarEffect(state.message, snackbarHostState, viewModel::clearMessage)

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.lists_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.lists_new))
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
                    com.dcapps.spese.ui.components.LoadingIndicator()
                state.error != null -> ErrorState(state.error!!, onRetry = viewModel::load)
                else -> ListContent(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onSortChange = viewModel::onSortChange,
                    onTogglePaid = viewModel::togglePaidListsShown,
                    onOpenList = onOpenList,
                    onDeleteList = { listToDelete = it },
                )
            }
        }
    }

    listToDelete?.let { list ->
        ConfirmDialog(
            title = stringResource(R.string.lists_delete_title),
            message = stringResource(R.string.lists_delete_message, list.name),
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
    onSortChange: (ListSort) -> Unit,
    onTogglePaid: () -> Unit,
    onOpenList: (ExpensesList) -> Unit,
    onDeleteList: (ExpensesList) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.lists_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
        }
        item {
            com.dcapps.spese.ui.components.SortBar(
                options = listOf(
                    com.dcapps.spese.ui.components.SortOption(ListSort.DATE, stringResource(R.string.sort_date)),
                    com.dcapps.spese.ui.components.SortOption(ListSort.NAME, stringResource(R.string.sort_name)),
                ),
                selectedKey = state.sort,
                ascending = state.sortAsc,
                onSelect = onSortChange,
            )
        }

        if (state.activeLists.isEmpty() && state.paidLists.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.lists_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }

        if (state.activeLists.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.lists_section_active)) }
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
                    SectionHeader(stringResource(R.string.lists_section_paid))
                    TextButton(onClick = onTogglePaid) {
                        Text(
                            if (state.paidListsShown) stringResource(R.string.lists_hide)
                            else stringResource(R.string.lists_show),
                        )
                    }
                }
            }
            if (state.paidListsShown) {
                items(state.paidLists, key = { it.id }) { list ->
                    ListCard(list, onOpenList, onDeleteList)
                }
            }
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
                val typeLabel =
                    if (list.type == com.dcapps.spese.core.model.ListType.SHARED)
                        stringResource(R.string.lists_type_shared)
                    else stringResource(R.string.lists_type_personal)
                val participantsLabel = list.participantsCount?.let {
                    stringResource(R.string.lists_participants, it)
                }
                val expensesLabel = list.expensesCount?.let {
                    stringResource(R.string.lists_expenses_count, it)
                }
                val subtitle = listOfNotNull(typeLabel, participantsLabel, expensesLabel)
                    .joinToString(" · ")
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { onDeleteList(list) }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
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
            Text(stringResource(R.string.action_retry))
        }
    }
}
