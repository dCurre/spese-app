package com.dcapps.spese.feature.expenses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.dcapps.spese.ui.components.ErrorState
import com.dcapps.spese.ui.components.RefreshOnResume
import com.dcapps.spese.ui.components.IconCount
import com.dcapps.spese.ui.components.ListRowContent
import com.dcapps.spese.ui.components.RowDivider
import com.dcapps.spese.ui.components.SelectableRow
import com.dcapps.spese.ui.components.SelectionTopBar
import com.dcapps.spese.core.ui.SelectionUiState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dcapps.spese.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.core.model.ListType
import com.dcapps.spese.navigation.OnCreateRequested
import com.dcapps.spese.navigation.Routes
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
    val sel by viewModel.selection.state.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<ExpensesList?>(null) }
    var showDeleteSelected by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Messaggio quando un guest prova a creare una lista condivisa (opzione lucchettata).
    val sharedLockedMsg = stringResource(R.string.guest_shared_list_locked)

    ErrorSnackbarEffect(state.message, snackbarHostState, viewModel::clearMessage)

    // Il "+" centrale della bottom bar apre il dialog di creazione di questa schermata.
    OnCreateRequested(Routes.EXPENSES_LIST) {
        showCreateDialog = true
    }

    // In modalità selezione, il tasto Indietro annulla la selezione invece di uscire.
    BackHandler(enabled = sel.selectionMode) { viewModel.selection.clear() }

    // Rinfresca al ritorno dal dettaglio (es. spese aggiunte/modificate → totali e conteggi).
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
                SpeseTopAppBar(title = stringResource(R.string.lists_title))
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
                state.isLoading && state.activeLists.isEmpty() && state.paidLists.isEmpty() ->
                    LoadingIndicator()
                state.error != null -> ErrorState(state.error!!, onRetry = viewModel::load)
                else -> ListContent(
                    state = state,
                    sel = sel,
                    onQueryChange = viewModel::onQueryChange,
                    onSortChange = viewModel::onSortChange,
                    onTogglePaid = viewModel::togglePaidListsShown,
                    onOpenList = onOpenList,
                    onDeleteList = { listToDelete = it },
                    onLongPress = { viewModel.selection.start(it.id) },
                    onToggleSelect = { viewModel.selection.toggle(it.id) },
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

    // Conferma eliminazione multipla (issue #10).
    if (showDeleteSelected) {
        ConfirmDialog(
            title = stringResource(R.string.lists_delete_selected_title),
            message = stringResource(R.string.lists_delete_selected_message, sel.selectedIds.size),
            onConfirm = { viewModel.deleteSelected() },
            onDismiss = { showDeleteSelected = false },
        )
    }

    if (showCreateDialog) {
        CreateListDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, shared ->
                viewModel.createList(name, shared)
                showCreateDialog = false
            },
            isGuest = viewModel.isGuest,
            onSharedLocked = {
                scope.launch { snackbarHostState.showSnackbar(sharedLockedMsg) }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListContent(
    state: ExpensesListUiState,
    sel: SelectionUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (ListSort) -> Unit,
    onTogglePaid: () -> Unit,
    onOpenList: (ExpensesList) -> Unit,
    onDeleteList: (ExpensesList) -> Unit,
    onLongPress: (ExpensesList) -> Unit,
    onToggleSelect: (ExpensesList) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
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
                SelectableListRow(list, sel, onOpenList, onDeleteList, onLongPress, onToggleSelect)
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
                    SelectableListRow(list, sel, onOpenList, onDeleteList, onLongPress, onToggleSelect)
                    RowDivider()
                }
            }
        }
    }
}

/** Riga lista con selezione multipla: swipe-to-delete solo fuori dalla modalità selezione. */
@Composable
private fun SelectableListRow(
    list: ExpensesList,
    sel: SelectionUiState,
    onOpenList: (ExpensesList) -> Unit,
    onDeleteList: (ExpensesList) -> Unit,
    onLongPress: (ExpensesList) -> Unit,
    onToggleSelect: (ExpensesList) -> Unit,
) {
    val rowContent: @Composable () -> Unit = {
        SelectableRow(
            selectionMode = sel.selectionMode,
            selected = list.id in sel.selectedIds,
            onClick = { onOpenList(list) },
            onLongClick = { onLongPress(list) },
            onToggle = { onToggleSelect(list) },
        ) {
            ExpensesRowContent(list)
        }
    }
    if (sel.selectionMode) {
        rowContent()
    } else {
        SwipeToDeleteRow(onRequestDelete = { onDeleteList(list) }) { rowContent() }
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


/** Contenuto della riga lista (dentro lo scope di [SelectableRow], che fornisce Row/click/checkbox). */
@Composable
private fun RowScope.ExpensesRowContent(list: ExpensesList) {
    val isShared = list.type == ListType.SHARED
    ListRowContent(
        title = list.name,
        subtitle = {
            // Tipo lista come badge colorato (Condivisa = accent, Personale = neutro).
            SpeseBadge(
                text = if (isShared) stringResource(R.string.lists_type_shared)
                    else stringResource(R.string.lists_type_personal),
                color = if (isShared) SpeseTheme.colors.accent
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Numero spese (prima) e, solo se condivisa, numero partecipanti: icona + conteggio.
            list.expensesCount?.let {
                IconCount(
                    icon = Icons.Default.ReceiptLong,
                    text = "$it",
                    contentDescription = stringResource(R.string.lists_expenses_cd),
                )
            }
            if (isShared) {
                list.participantsCount?.let {
                    IconCount(
                        icon = Icons.Default.Group,
                        text = "$it",
                        contentDescription = stringResource(R.string.lists_participants_cd),
                    )
                }
            }
        },
    )
}

