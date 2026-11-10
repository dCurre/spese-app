package com.dcapps.spese.feature.checklist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.R
import com.dcapps.spese.core.model.ShoppingList
import com.dcapps.spese.feature.expenses.CreateListDialog
import com.dcapps.spese.navigation.OnCreateRequested
import com.dcapps.spese.navigation.Routes
import com.dcapps.spese.ui.components.ConfirmDialog
import com.dcapps.spese.ui.components.ErrorSnackbarEffect
import com.dcapps.spese.ui.components.ErrorState
import com.dcapps.spese.ui.components.IconCount
import com.dcapps.spese.ui.components.ListRowContent
import com.dcapps.spese.ui.components.PendingSyncBadge
import com.dcapps.spese.ui.components.RowDivider
import com.dcapps.spese.ui.components.SortBar
import com.dcapps.spese.ui.components.SortOption
import com.dcapps.spese.ui.components.SpeseBadge
import com.dcapps.spese.ui.components.SelectableRow
import com.dcapps.spese.ui.components.SelectionTopBar
import com.dcapps.spese.ui.components.SpeseSearchField
import com.dcapps.spese.ui.components.SpeseTopAppBar
import com.dcapps.spese.ui.components.SwipeToDeleteRow
import com.dcapps.spese.core.ui.SelectionUiState
import com.dcapps.spese.ui.theme.SpeseTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    onOpenList: (ShoppingList) -> Unit,
    viewModel: ChecklistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sel by viewModel.selection.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var showDeleteSelected by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Messaggi quando l'opzione "condivisa" è lucchettata: guest (serve account) o offline (serve rete).
    val sharedLockedGuestMsg = stringResource(R.string.guest_shared_list_locked)
    val sharedLockedOfflineMsg = stringResource(R.string.offline_shared_list_locked)
    ErrorSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
    )

    OnCreateRequested(Routes.CHECKLIST) {
        showCreate = true
    }

    // In modalità selezione, il tasto Indietro annulla la selezione invece di uscire.
    BackHandler(enabled = sel.selectionMode) { viewModel.selection.clear() }

    // (Local-first: nessun refresh al ritorno — la UI osserva Room e si aggiorna da sola.)

    Scaffold(
        topBar = {
            if (sel.selectionMode) {
                SelectionTopBar(
                    count = sel.selectedIds.size,
                    onCancel = { viewModel.selection.clear() },
                    onDelete = { showDeleteSelected = true },
                )
            } else {
                SpeseTopAppBar(title = stringResource(R.string.checklist_title))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.error != null -> ErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.load(showLoading = true) },
                )
                else -> ChecklistContent(state, sel, viewModel, onOpenList)
            }
        }
    }

    // Conferma eliminazione multipla (issue #10).
    if (showDeleteSelected) {
        ConfirmDialog(
            title = stringResource(R.string.checklist_lists_delete_selected_title),
            message = stringResource(R.string.checklist_lists_delete_selected_message, sel.selectedIds.size),
            onConfirm = { viewModel.deleteSelected() },
            onDismiss = { showDeleteSelected = false },
        )
    }

    if (showCreate) {
        CreateListDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, shared ->
                viewModel.create(name, shared)
                showCreate = false
            },
            sharedLocked = viewModel.isGuest || !state.isOnline,
            onSharedLocked = {
                val msg = if (viewModel.isGuest) sharedLockedGuestMsg else sharedLockedOfflineMsg
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
            titleRes = R.string.checklist_new,
        )
    }
}

@Composable
private fun ChecklistContent(
    state: ChecklistUiState,
    sel: SelectionUiState,
    viewModel: ChecklistViewModel,
    onOpenList: (ShoppingList) -> Unit,
) {
    val starredTitle = stringResource(R.string.checklist_section_starred)
    val activeTitle = stringResource(R.string.checklist_section_active)
    val completedTitle = stringResource(R.string.checklist_section_completed)
    var listToDelete by remember { mutableStateOf<ShoppingList?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
    ) {
        item {
            SpeseSearchField(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.lists_search),
            )
        }
        item {
            SortBar(
                options = listOf(
                    SortOption(ChecklistSort.DATE, stringResource(R.string.sort_date)),
                    SortOption(ChecklistSort.NAME, stringResource(R.string.sort_name)),
                ),
                selectedKey = state.sort,
                ascending = state.sortAsc,
                onSelect = viewModel::onSortChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (state.starred.isEmpty() && state.active.isEmpty() && state.completed.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.lists_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        section(starredTitle, state.starred, sel, viewModel, onOpenList, state.showPendingSync, onRequestDelete = { listToDelete = it })
        section(activeTitle, state.active, sel, viewModel, onOpenList, state.showPendingSync, onRequestDelete = { listToDelete = it })
        section(completedTitle, state.completed, sel, viewModel, onOpenList, state.showPendingSync, onRequestDelete = { listToDelete = it })
    }

    listToDelete?.let { list ->
        ConfirmDialog(
            title = stringResource(R.string.checklist_delete_title),
            message = stringResource(R.string.checklist_delete_message, list.name),
            onConfirm = { viewModel.delete(list) },
            onDismiss = { listToDelete = null },
        )
    }
}

private fun LazyListScope.section(
    title: String,
    lists: List<ShoppingList>,
    sel: SelectionUiState,
    viewModel: ChecklistViewModel,
    onOpenList: (ShoppingList) -> Unit,
    showPendingSync: Boolean,
    onRequestDelete: (ShoppingList) -> Unit,
) {
    if (lists.isEmpty()) return
    item(key = "header-$title") {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
    items(lists, key = { it.id }) { list ->
        SwipeableChecklistRow(
            list = list,
            sel = sel,
            onOpenList = onOpenList,
            onStar = { viewModel.toggleStarred(list) },
            onRequestDelete = { onRequestDelete(list) },
            onLongPress = { viewModel.selection.start(list.id) },
            onToggleSelect = { viewModel.selection.toggle(list.id) },
            showPendingSync = showPendingSync,
        )
        RowDivider()
    }
}

/**
 * Riga checklist con selezione multipla: fuori dalla selezione ha lo swipe-to-delete (l'eliminazione
 * effettiva passa dal dialog di conferma); in selezione usa checkbox/evidenziazione.
 */
@Composable
private fun SwipeableChecklistRow(
    list: ShoppingList,
    sel: SelectionUiState,
    onOpenList: (ShoppingList) -> Unit,
    onStar: () -> Unit,
    onRequestDelete: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelect: () -> Unit,
    showPendingSync: Boolean,
) {
    val rowContent: @Composable () -> Unit = {
        SelectableRow(
            selectionMode = sel.selectionMode,
            selected = list.id in sel.selectedIds,
            onClick = { onOpenList(list) },
            onLongClick = onLongPress,
            onToggle = onToggleSelect,
        ) {
            ChecklistRowContent(list, sel.selectionMode, onStar, showPendingSync)
        }
    }
    if (sel.selectionMode) {
        rowContent()
    } else {
        SwipeToDeleteRow(onRequestDelete = onRequestDelete) { rowContent() }
    }
}

/** Contenuto riga checklist (dentro [SelectableRow]). La stella è nascosta in modalità selezione. */
@Composable
private fun RowScope.ChecklistRowContent(
    list: ShoppingList,
    selectionMode: Boolean,
    onStar: () -> Unit,
    showPendingSync: Boolean,
) {
    val total = list.itemsCount ?: 0
    val checked = list.checkedCount ?: 0
    val isShared = list.listType == "shared"
    val countLabel = if (total > 0) "$checked/$total" else null
    ListRowContent(
        title = list.name,
        subtitle = {
            SpeseBadge(
                text = if (isShared) stringResource(R.string.lists_type_shared)
                    else stringResource(R.string.lists_type_personal),
                color = if (isShared) SpeseTheme.colors.accent
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Conteggio articoli (spuntati/totale) con icona, invece del solo testo.
            countLabel?.let {
                IconCount(
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    text = it,
                    contentDescription = stringResource(R.string.checklist_items_cd),
                )
            }
            // Badge "in attesa di sync" se la lista ha modifiche locali non inviate (punto 2).
            PendingSyncBadge(show = showPendingSync && list.pendingSync)
        },
        trailing = if (!selectionMode) {
            {
                IconButton(onClick = onStar) {
                    if (list.starred) {
                        Icon(Icons.Default.Star, contentDescription = stringResource(R.string.checklist_star_remove), tint = SpeseTheme.colors.accent)
                    } else {
                        Icon(Icons.Outlined.StarBorder, contentDescription = stringResource(R.string.checklist_star_add), tint = SpeseTheme.colors.accent)
                    }
                }
            }
        } else null,
    )
}
