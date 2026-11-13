package com.dcapps.spese.feature.checklist

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.R
import com.dcapps.spese.core.model.ShoppingCategory
import com.dcapps.spese.core.model.ShoppingItem
import com.dcapps.spese.ui.components.ConfirmDialog
import com.dcapps.spese.ui.components.ErrorSnackbarEffect
import com.dcapps.spese.ui.components.OverflowMenu
import com.dcapps.spese.ui.components.PendingSyncBadge
import com.dcapps.spese.ui.components.ReadOnlyPickerField
import com.dcapps.spese.ui.components.SortBar
import com.dcapps.spese.ui.components.SpeseBadge
import com.dcapps.spese.ui.components.SortOption
import com.dcapps.spese.ui.components.SelectionTopBar
import com.dcapps.spese.ui.components.SpeseDialog
import com.dcapps.spese.ui.components.SpeseSearchField
import com.dcapps.spese.ui.components.SpeseTopAppBar
import com.dcapps.spese.ui.theme.SpeseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistDetailScreen(
    onBack: () -> Unit,
    viewModel: ChecklistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sel by viewModel.selection.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var newItem by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var editingCategory by remember { mutableStateOf<ShoppingCategory?>(null) }
    var itemToDelete by remember { mutableStateOf<ShoppingItem?>(null) }
    var categoryToDelete by remember { mutableStateOf<ShoppingCategory?>(null) }
    var showNewCategory by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var showDeleteList by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showMakePersonal by remember { mutableStateOf(false) }
    var showDeleteSelectedItems by remember { mutableStateOf(false) }
    // Dialog "sposta articoli selezionati in categoria" (issue #17).
    var showAssignCategory by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    ErrorSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
        reportable = state.messageReportable,
    )

    // Torna indietro quando la checklist viene eliminata.
    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    // In modalità selezione, il tasto Indietro annulla la selezione invece di uscire.
    BackHandler(enabled = sel.selectionMode) { viewModel.selection.clear() }

    // Condivisione: quando arriva un token, apre il selettore di condivisione di sistema.
    val token = state.inviteToken
    if (token != null) {
        LaunchedShare(token = token, onShared = viewModel::clearInviteToken)
    }

    val editState = state.editState

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = if (sel.selectionMode) {
            {
                SelectionTopBar(
                    count = sel.selectedIds.size,
                    onCancel = { viewModel.selection.clear() },
                    onDelete = { showDeleteSelectedItems = true },
                    onAssignCategory = { showAssignCategory = true },
                )
            }
        } else {
            {
            SpeseTopAppBar(
                title = if (editState != null) stringResource(R.string.cd_edit_list)
                    else state.displayName ?: stringResource(R.string.detail_list_fallback),
                // Matita accanto al nome = rinomina classica della lista (come nelle altre sezioni).
                onTitleEdit = if (editState == null) { { showRename = true } } else null,
                navigationIcon = {
                    IconButton(onClick = { if (editState != null) viewModel.cancelEdit() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (editState == null) {
                        // Toggle unico comprimi/espandi tutto (solo se ci sono categorie).
                        if (state.hasCategories) {
                            IconButton(onClick = { viewModel.toggleCollapseAll() }) {
                                Icon(
                                    imageVector = if (state.allCollapsed) Icons.Default.UnfoldMore
                                        else Icons.Default.UnfoldLess,
                                    contentDescription = stringResource(
                                        if (state.allCollapsed) R.string.cd_menu_expand_all
                                        else R.string.cd_menu_collapse_all,
                                    ),
                                )
                            }
                        }
                        // Preferito (stella), staccata dal menu con un piccolo spazio.
                        IconButton(onClick = { viewModel.toggleStarred() }) {
                            Icon(
                                imageVector = if (state.starred) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = stringResource(
                                    if (state.starred) R.string.checklist_star_remove
                                    else R.string.checklist_star_add,
                                ),
                                tint = SpeseTheme.colors.accent,
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        val exportChooserTitle = stringResource(R.string.cd_export_chooser)
                        ChecklistOverflowMenu(
                            viewModel,
                            hasItems = state.hasItems,
                            // Cambio tipo solo per l'owner (issue #11).
                            canChangeType = viewModel.isOwner,
                            isShared = state.isShared,
                            onMakeShared = { viewModel.setListType(true) },
                            onMakePersonal = { showMakePersonal = true },
                            onBulkEdit = { viewModel.enterEditMode() },
                            onExport = {
                                val text = viewModel.buildExportText()
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(intent, exportChooserTitle),
                                )
                            },
                            onImport = { showImport = true },
                            onDelete = { showDeleteList = true },
                        )
                    }
                },
            )
            }
        },
        bottomBar = {
            if (editState != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = viewModel::cancelEdit, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = viewModel::saveEdits,
                        enabled = !state.saving,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (state.saving) stringResource(R.string.cd_saving)
                            else stringResource(R.string.action_save),
                        )
                    }
                }
            } else if (!sel.selectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newItem,
                        onValueChange = { newItem = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.editor_add_item)) },
                        singleLine = true,
                    )
                    IconButton(
                        onClick = { viewModel.addItem(newItem, null); newItem = "" },
                        enabled = newItem.isNotBlank(),
                    ) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add)) }
                    // Aggiunta rapida categoria, accanto al "+" item.
                    IconButton(onClick = { showNewCategory = true }) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = stringResource(R.string.editor_new_category),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, Modifier.align(Alignment.Center).padding(32.dp))
                editState != null -> ChecklistEditor(
                    editState = editState,
                    onEdit = viewModel::editLocal,
                )
                // Lista del tutto vuota (nessun item e nessuna categoria): messaggio, niente barra.
                state.list?.let { it.items.isEmpty() && it.categories.isEmpty() } == true -> Text(
                    stringResource(R.string.cd_empty),
                    Modifier.align(Alignment.Center).padding(32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    // Barra di ricerca + ordinamento (fisse in cima, scorrono con la lista).
                    item {
                        SpeseSearchField(
                            query = state.query,
                            onQueryChange = viewModel::onQueryChange,
                            placeholder = stringResource(R.string.cd_search_item),
                        )
                    }
                    item {
                        SortBar(
                            options = listOf(
                                SortOption(ChecklistItemSort.ORDER, stringResource(R.string.cd_sort_order)),
                                SortOption(ChecklistItemSort.NAME, stringResource(R.string.sort_name)),
                                SortOption(ChecklistItemSort.CHECKED, stringResource(R.string.cd_sort_checked)),
                            ),
                            selectedKey = state.sort,
                            ascending = state.sortAsc,
                            onSelect = viewModel::onSortChange,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    if (state.rows.isEmpty()) {
                        item {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(R.string.cd_no_results),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                // Se la ricerca non trova nulla, proponi di creare l'articolo cercato. (issue #6)
                                if (state.query.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            viewModel.addItem(state.query, null)
                                            viewModel.onQueryChange("")
                                        },
                                        contentPadding = PaddingValues(0.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(R.string.cd_create_searched, state.query.trim()))
                                    }
                                }
                            }
                        }
                    }
                    items(state.rows, key = { rowKey(it) }) { row ->
                        when (row) {
                            is ChecklistRow.CategoryHeader -> CategoryHeaderRow(
                                row = row,
                                onToggleCollapse = { viewModel.toggleCategoryCollapsed(row.category.id) },
                                onEdit = { editingCategory = row.category },
                                onDelete = { categoryToDelete = row.category },
                            )
                            is ChecklistRow.Item -> ItemRow(
                                row = row,
                                selectionMode = sel.selectionMode,
                                selected = row.item.id in sel.selectedIds,
                                onToggle = { viewModel.toggleItem(row.item) },
                                onEdit = { editingItem = row.item },
                                onLongPress = { viewModel.selection.start(row.item.id) },
                                onToggleSelect = { viewModel.selection.toggle(row.item.id) },
                                showPendingSync = state.showPendingSync,
                            )
                        }
                    }
                }
            }
        }
    }

    editingItem?.let { item ->
        ItemEditDialog(
            item = item,
            categories = state.categories,
            onDismiss = { editingItem = null },
            onDelete = { itemToDelete = item; editingItem = null },
            onConfirm = { name, qty, catId ->
                viewModel.updateItem(item, name, qty, catId)
                editingItem = null
            },
        )
    }

    itemToDelete?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.editor_delete_item),
            message = stringResource(R.string.cd_delete_item_message, item.name),
            onConfirm = { viewModel.deleteItem(item) },
            onDismiss = { itemToDelete = null },
        )
    }

    // Conferma eliminazione multipla articoli (issue #10).
    if (showDeleteSelectedItems) {
        ConfirmDialog(
            title = stringResource(R.string.checklist_items_delete_selected_title),
            message = stringResource(R.string.checklist_items_delete_selected_message, sel.selectedIds.size),
            onConfirm = { viewModel.deleteSelectedItems() },
            onDismiss = { showDeleteSelectedItems = false },
        )
    }

    // Sposta articoli selezionati in una categoria (issue #17). "Senza categoria" = fuori da ogni
    // categoria (null). La destinazione attuale (se condivisa da tutti i selezionati) è esclusa.
    if (showAssignCategory) {
        val common = viewModel.commonCategoryOfSelection()
        val excludedId = (common as? CommonCategory.Single)?.categoryId
        // Nascondi "senza categoria" solo se TUTTI i selezionati sono già senza categoria.
        val showNoneOption = !(common is CommonCategory.Single && common.categoryId == null)
        AssignCategoryDialog(
            categories = state.categories.filter { it.id != excludedId },
            showNoneOption = showNoneOption,
            count = sel.selectedIds.size,
            onDismiss = { showAssignCategory = false },
            onConfirm = { categoryId ->
                viewModel.assignSelectedToCategory(categoryId)
                showAssignCategory = false
            },
        )
    }

    editingCategory?.let { cat ->
        // Parent ammessi: tutte le categorie tranne la categoria stessa e i suoi discendenti
        // (evita cicli).
        val blocked = viewModel.descendantIds(cat.id)
        val availableParents = state.categories.filter { it.id !in blocked }
        CategoryEditDialog(
            category = cat,
            availableParents = availableParents,
            onDismiss = { editingCategory = null },
            onDelete = { categoryToDelete = cat; editingCategory = null },
            onConfirm = { name, parentId ->
                viewModel.updateCategory(cat, name, parentId); editingCategory = null
            },
        )
    }

    categoryToDelete?.let { cat ->
        ConfirmDialog(
            title = stringResource(R.string.cd_delete_category_title),
            message = stringResource(R.string.cd_delete_category_message, cat.name),
            onConfirm = { viewModel.deleteCategory(cat) },
            onDismiss = { categoryToDelete = null },
        )
    }

    if (showNewCategory) {
        NewCategoryDialog(
            categories = state.categories,
            onDismiss = { showNewCategory = false },
            onCreate = { name, parentId -> viewModel.addCategory(name, parentId); showNewCategory = false },
        )
    }

    if (showImport) {
        ImportDialog(
            onDismiss = { showImport = false },
            onImport = { text -> viewModel.importFromText(text); showImport = false },
        )
    }

    if (showDeleteList) {
        ConfirmDialog(
            title = stringResource(R.string.checklist_delete_title),
            message = stringResource(R.string.checklist_delete_message, state.list?.name ?: ""),
            onConfirm = { viewModel.deleteList() },
            onDismiss = { showDeleteList = false },
        )
    }

    if (showRename) {
        RenameListDialog(
            currentName = state.list?.name ?: state.displayName ?: "",
            onDismiss = { showRename = false },
            onConfirm = { viewModel.renameList(it); showRename = false },
        )
    }

    // Conferma passaggio a personale (distruttivo: rimuove i partecipanti). (issue #11)
    if (showMakePersonal) {
        ConfirmDialog(
            title = stringResource(R.string.list_make_personal_title),
            message = stringResource(R.string.checklist_make_personal_message),
            confirmText = stringResource(R.string.list_make_personal),
            onConfirm = { viewModel.setListType(false) },
            onDismiss = { showMakePersonal = false },
        )
    }

}

@Composable
private fun RenameListDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    SpeseDialog(
        title = stringResource(R.string.cd_rename_list),
        onDismiss = onDismiss,
        icon = Icons.Outlined.DriveFileRenameOutline,
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onConfirm(name) },
        confirmEnabled = name.isNotBlank(),
    )
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    SpeseDialog(
        title = stringResource(R.string.cd_import_title),
        onDismiss = onDismiss,
        icon = Icons.Outlined.FileDownload,
        content = {
            Text(
                stringResource(R.string.cd_import_help),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                placeholder = { Text(stringResource(R.string.cd_import_placeholder)) },
                minLines = 4,
            )
        },
        confirmText = stringResource(R.string.cd_import_confirm),
        onConfirm = { onImport(text) },
        confirmEnabled = text.isNotBlank(),
    )
}

@Composable
private fun LaunchedShare(token: String, onShared: () -> Unit) {
    val context = LocalContext.current
    val link = "https://spese-dc.web.app/checklist/join/$token"
    val shareText = stringResource(R.string.cd_share_text, link)
    val chooserTitle = stringResource(R.string.detail_share_chooser)
    LaunchedEffect(token) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
        onShared()
    }
}

@Composable
private fun ChecklistOverflowMenu(
    viewModel: ChecklistDetailViewModel,
    hasItems: Boolean,
    canChangeType: Boolean,
    isShared: Boolean,
    onMakeShared: () -> Unit,
    onMakePersonal: () -> Unit,
    onBulkEdit: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDelete: () -> Unit,
) {
    OverflowMenu { dismiss ->
        // Modifica lista (bulk edit: riordino/editing multiplo di categorie e articoli).
        DropdownMenuItem(
            text = { Text(stringResource(R.string.cd_edit_list)) },
            onClick = { dismiss(); onBulkEdit() },
        )
        // Cambio tipo condivisa↔personale, solo per l'owner (issue #11).
        if (canChangeType) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(
                        if (isShared) R.string.list_make_personal else R.string.list_make_shared,
                    ))
                },
                onClick = { dismiss(); if (isShared) onMakePersonal() else onMakeShared() },
            )
        }
        // Spunta/despunta tutto come due voci separate (solo se ci sono articoli).
        if (hasItems) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.cd_menu_check_all)) },
                onClick = { dismiss(); viewModel.checkAll(true) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.cd_menu_uncheck_all)) },
                onClick = { dismiss(); viewModel.checkAll(false) },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.detail_share)) },
            onClick = { dismiss(); viewModel.generateInviteToken() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.cd_menu_export)) },
            onClick = { dismiss(); onExport() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.cd_import_confirm)) },
            onClick = { dismiss(); onImport() },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(R.string.checklist_delete_title),
                    color = SpeseTheme.colors.danger,
                )
            },
            onClick = { dismiss(); onDelete() },
        )
    }
}

private fun rowKey(row: ChecklistRow): String = when (row) {
    is ChecklistRow.CategoryHeader -> "c${row.category.id}"
    is ChecklistRow.Item -> "i${row.item.id}"
}

@Composable
private fun CategoryHeaderRow(
    row: ChecklistRow.CategoryHeader,
    onToggleCollapse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    TreeIndent(
        depth = row.depth,
        isLastChild = row.isLastChild,
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Freccetta larga quanto una colonna di indentazione (28dp) e centrata: così la guida
            // verticale dei figli cade esattamente sotto il centro di questa freccetta.
            IconButton(onClick = onToggleCollapse, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (row.collapsed) {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (row.collapsed) stringResource(R.string.cd_expand) else stringResource(R.string.cd_collapse),
                    modifier = Modifier.size(20.dp),
                )
            }
            // Tap sul nome = collassa/espande (come la freccetta). Nome barrato se completata.
            Text(
                text = row.category.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (row.completed) TextDecoration.LineThrough else null,
                color = if (row.completed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { onToggleCollapse() }.padding(vertical = 8.dp),
            )
            // Badge: se la categoria contiene articoli mostra "spuntati/totale" (es. 2/3, issue #21);
            // altrimenti (nessun articolo, solo sottocategorie) il numero di sottoelementi diretti.
            SpeseBadge(
                text = if (row.totalItems > 0) "${row.checkedItems}/${row.totalItems}" else "${row.directCount}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit_category))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.editor_delete_category), tint = SpeseTheme.colors.danger)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemRow(
    row: ChecklistRow.Item,
    selectionMode: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelect: () -> Unit,
    showPendingSync: Boolean,
) {
    val item = row.item
    // L'alberatura (TreeIndent) resta: per non rompere l'indentazione né mettere due checkbox,
    // la selezione evidenzia la riga e intercetta tap/long-press; in selezione il checkbox a
    // sinistra è quello di SELEZIONE, fuori è quello di SPUNTA articolo.
    TreeIndent(
        depth = row.depth,
        isLastChild = row.isLastChild,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else androidx.compose.ui.graphics.Color.Transparent,
            )
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() else onEdit() },
                onLongClick = onLongPress,
            )
            .padding(start = 8.dp, end = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
            } else {
                Checkbox(checked = item.checked, onCheckedChange = { onToggle() })
            }
            Text(
                text = buildString {
                    append(item.name)
                    val q = item.quantity ?: 1
                    if (q > 1) append("  ×$q")
                },
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            )
            // Badge "in attesa di sync" se l'articolo ha modifiche locali non inviate (punto 2).
            PendingSyncBadge(show = showPendingSync && item.pendingSync)
        }
    }
}

/**
 * Dialog per spostare gli articoli selezionati in una categoria (issue #17).
 *  - [categories] sono le destinazioni possibili (la destinazione attuale è già esclusa a monte);
 *  - [showNoneOption] aggiunge "Senza categoria" (fuori da ogni categoria); è nascosto se tutti i
 *    selezionati sono già senza categoria.
 * Conferma abilitata solo quando una destinazione è selezionata (null è una scelta valida solo se
 * è stata scelta esplicitamente "Senza categoria").
 */
@Composable
private fun AssignCategoryDialog(
    categories: List<ShoppingCategory>,
    showNoneOption: Boolean,
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long?) -> Unit,
) {
    // Sentinella: nessuna scelta ancora fatta (diverso da "Senza categoria" = null).
    val unselected = Long.MIN_VALUE
    val noneValue = -1L
    var choice by remember { mutableStateOf(unselected) }
    val noneLabel = stringResource(R.string.cd_move_no_category)
    val options = buildList<Pair<Long, String>> {
        if (showNoneOption) add(noneValue to noneLabel)
        addAll(categories.map { it.id to it.name })
    }
    SpeseDialog(
        title = stringResource(R.string.cd_move_to_category),
        onDismiss = onDismiss,
        icon = Icons.Default.DriveFileMove,
        content = {
            Text(stringResource(R.string.cd_move_message, count))
            ReadOnlyPickerField(
                label = stringResource(R.string.cd_parent_category),
                options = options,
                selectedId = choice.takeIf { it != unselected },
                onSelect = { choice = it },
            )
        },
        confirmText = stringResource(R.string.action_move),
        onConfirm = { onConfirm(if (choice == noneValue) null else choice) },
        confirmEnabled = choice != unselected,
    )
}

@Composable
private fun NewCategoryDialog(
    categories: List<ShoppingCategory>,
    onDismiss: () -> Unit,
    onCreate: (name: String, parentId: Long?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<Long?>(null) }
    SpeseDialog(
        title = stringResource(R.string.editor_new_category),
        onDismiss = onDismiss,
        icon = Icons.Default.CreateNewFolder,
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            // Scelta categoria padre (opzionale) → crea una sottocategoria.
            if (categories.isNotEmpty()) {
                ParentCategoryPicker(
                    categories = categories,
                    selectedId = parentId,
                    onSelect = { parentId = it },
                )
            }
        },
        confirmText = stringResource(R.string.action_create),
        onConfirm = { onCreate(name, parentId) },
        confirmEnabled = name.isNotBlank(),
    )
}

@Composable
private fun ParentCategoryPicker(
    categories: List<ShoppingCategory>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    val noneLabel = stringResource(R.string.cd_parent_none)
    ReadOnlyPickerField(
        label = stringResource(R.string.cd_parent_category),
        options = listOf<Pair<Long?, String>>(null to noneLabel) + categories.map { it.id to it.name },
        selectedId = selectedId,
        onSelect = onSelect,
    )
}

@Composable
private fun CategoryEditDialog(
    category: ShoppingCategory,
    availableParents: List<ShoppingCategory>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: (name: String, parentId: Long?) -> Unit,
) {
    var name by remember { mutableStateOf(category.name) }
    var parentId by remember { mutableStateOf(category.parentId) }
    SpeseDialog(
        title = stringResource(R.string.cd_category_title),
        onDismiss = onDismiss,
        icon = Icons.Default.CreateNewFolder,
        // Elimina in basso a sinistra (danger), separato da Annulla/Salva a destra.
        onDelete = onDelete,
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ParentCategoryPicker(
                categories = availableParents,
                selectedId = parentId,
                onSelect = { parentId = it },
            )
        },
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onConfirm(name, parentId) },
        confirmEnabled = name.isNotBlank(),
    )
}
