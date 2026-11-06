package com.dcapps.spese.feature.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.dcapps.spese.ui.components.ErrorState
import com.dcapps.spese.ui.components.RowDivider
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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.dcapps.spese.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.ui.components.ConfirmDialog
import com.dcapps.spese.ui.components.ErrorSnackbarEffect
import com.dcapps.spese.ui.components.LoadingIndicator
import com.dcapps.spese.ui.components.SortBar
import com.dcapps.spese.ui.components.SortOption
import com.dcapps.spese.ui.components.SpeseBadge
import com.dcapps.spese.ui.components.SpeseSearchField
import com.dcapps.spese.ui.components.SpeseTopAppBar
import com.dcapps.spese.ui.components.SwipeToDeleteRow
import com.dcapps.spese.ui.theme.SpeseTheme

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

    // Il "+" centrale della bottom bar apre il dialog di creazione di questa schermata.
    com.dcapps.spese.navigation.OnCreateRequested(com.dcapps.spese.navigation.Routes.EXPENSES_LIST) {
        showCreateDialog = true
    }

    Scaffold(
        topBar = { SpeseTopAppBar(title = stringResource(R.string.lists_title)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.load(showLoading = true) },
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            when {
                state.isLoading && state.activeLists.isEmpty() && state.paidLists.isEmpty() ->
                    LoadingIndicator()
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 88.dp),
    ) {
        item {
            SpeseSearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(R.string.lists_search),
            )
        }
        item {
            SortBar(
                options = listOf(
                    SortOption(ListSort.DATE, stringResource(R.string.sort_date)),
                    SortOption(ListSort.NAME, stringResource(R.string.sort_name)),
                ),
                selectedKey = state.sort,
                ascending = state.sortAsc,
                onSelect = onSortChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (state.activeLists.isEmpty() && state.paidLists.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.lists_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        if (state.activeLists.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.lists_section_active)) }
            items(state.activeLists, key = { it.id }) { list ->
                SwipeToDeleteRow(onRequestDelete = { onDeleteList(list) }) {
                    ListRow(list, onOpenList)
                }
                RowDivider()
            }
        }

        if (state.paidLists.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
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
                    SwipeToDeleteRow(onRequestDelete = { onDeleteList(list) }) {
                        ListRow(list, onOpenList)
                    }
                    RowDivider()
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}


@Composable
private fun ListRow(
    list: ExpensesList,
    onOpenList: (ExpensesList) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable { onOpenList(list) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(list.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val isShared = list.type == com.dcapps.spese.core.model.ListType.SHARED
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Tipo lista come badge colorato (Condivisa = accent, Personale = neutro).
                SpeseBadge(
                    text = if (isShared) stringResource(R.string.lists_type_shared)
                        else stringResource(R.string.lists_type_personal),
                    color = if (isShared) SpeseTheme.colors.accent
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Partecipanti e numero spese come icona + conteggio (meno verboso del testo).
                list.participantsCount?.let {
                    com.dcapps.spese.ui.components.IconCount(
                        icon = Icons.Default.Group,
                        text = "$it",
                        contentDescription = stringResource(R.string.lists_participants_cd),
                    )
                }
                list.expensesCount?.let {
                    com.dcapps.spese.ui.components.IconCount(
                        icon = Icons.Default.ReceiptLong,
                        text = "$it",
                        contentDescription = stringResource(R.string.lists_expenses_cd),
                    )
                }
            }
        }
    }
}

