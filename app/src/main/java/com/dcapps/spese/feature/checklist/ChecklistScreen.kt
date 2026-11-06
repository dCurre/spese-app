package com.dcapps.spese.feature.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.dcapps.spese.core.model.ShoppingList
import com.dcapps.spese.feature.expenses.CreateListDialog
import com.dcapps.spese.ui.components.ConfirmDialog
import com.dcapps.spese.ui.components.ErrorSnackbarEffect
import com.dcapps.spese.ui.components.ErrorState
import com.dcapps.spese.ui.components.RowDivider
import com.dcapps.spese.ui.components.SortBar
import com.dcapps.spese.ui.components.SortOption
import com.dcapps.spese.ui.components.SpeseBadge
import com.dcapps.spese.ui.components.SpeseSearchField
import com.dcapps.spese.ui.components.SpeseTopAppBar
import com.dcapps.spese.ui.components.SwipeToDeleteRow
import com.dcapps.spese.ui.theme.SpeseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    onOpenList: (ShoppingList) -> Unit,
    viewModel: ChecklistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    ErrorSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
    )

    com.dcapps.spese.navigation.OnCreateRequested(com.dcapps.spese.navigation.Routes.CHECKLIST) {
        showCreate = true
    }

    Scaffold(
        topBar = { SpeseTopAppBar(title = stringResource(R.string.checklist_title)) },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.load(showLoading = true) },
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            when {
                state.error != null -> ErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.load(showLoading = true) },
                )
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
            titleRes = R.string.checklist_new,
        )
    }
}

@Composable
private fun ChecklistContent(
    state: ChecklistUiState,
    viewModel: ChecklistViewModel,
    onOpenList: (ShoppingList) -> Unit,
) {
    val starredTitle = stringResource(R.string.checklist_section_starred)
    val activeTitle = stringResource(R.string.checklist_section_active)
    val completedTitle = stringResource(R.string.checklist_section_completed)
    var listToDelete by remember { mutableStateOf<ShoppingList?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 88.dp),
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

        section(starredTitle, state.starred, viewModel, onOpenList, onRequestDelete = { listToDelete = it })
        section(activeTitle, state.active, viewModel, onOpenList, onRequestDelete = { listToDelete = it })
        section(completedTitle, state.completed, viewModel, onOpenList, onRequestDelete = { listToDelete = it })
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

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    lists: List<ShoppingList>,
    viewModel: ChecklistViewModel,
    onOpenList: (ShoppingList) -> Unit,
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
            onOpenList = onOpenList,
            onStar = { viewModel.toggleStarred(list) },
            onRequestDelete = { onRequestDelete(list) },
        )
        RowDivider()
    }
}

/**
 * Riga checklist con swipe-to-delete (sfondo rosso "Elimina"); l'eliminazione effettiva passa
 * dal dialog di conferma. Usa il componente condiviso [SwipeToDeleteRow].
 */
@Composable
private fun SwipeableChecklistRow(
    list: ShoppingList,
    onOpenList: (ShoppingList) -> Unit,
    onStar: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    SwipeToDeleteRow(onRequestDelete = onRequestDelete) {
        ChecklistRow(list, onOpenList, onStar = onStar)
    }
}

@Composable
private fun ChecklistRow(
    list: ShoppingList,
    onOpenList: (ShoppingList) -> Unit,
    onStar: () -> Unit,
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
            val total = list.itemsCount ?: 0
            val checked = list.checkedCount ?: 0
            val isShared = list.listType == "shared"
            val countLabel = if (total > 0) "$checked/$total" else null
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SpeseBadge(
                    text = if (isShared) stringResource(R.string.lists_type_shared)
                        else stringResource(R.string.lists_type_personal),
                    color = if (isShared) SpeseTheme.colors.accent
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Conteggio articoli (spuntati/totale) con icona, invece del solo testo.
                countLabel?.let {
                    com.dcapps.spese.ui.components.IconCount(
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        text = it,
                        contentDescription = stringResource(R.string.checklist_items_cd),
                    )
                }
            }
        }
        IconButton(onClick = onStar) {
            if (list.starred) {
                Icon(Icons.Default.Star, contentDescription = stringResource(R.string.checklist_star_remove), tint = SpeseTheme.colors.accent)
            } else {
                Icon(Icons.Outlined.StarBorder, contentDescription = stringResource(R.string.checklist_star_add), tint = SpeseTheme.colors.accent)
            }
        }
    }
}
