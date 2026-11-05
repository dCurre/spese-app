package com.dcapps.spese.feature.checklist

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistDetailScreen(
    onBack: () -> Unit,
    viewModel: ChecklistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var newItem by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var editingCategory by remember { mutableStateOf<ShoppingCategory?>(null) }
    var itemToDelete by remember { mutableStateOf<ShoppingItem?>(null) }
    var categoryToDelete by remember { mutableStateOf<ShoppingCategory?>(null) }
    var showNewCategory by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    com.dcapps.spese.ui.components.ErrorSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
    )

    // Condivisione: quando arriva un token, apre il selettore di condivisione di sistema.
    val token = state.inviteToken
    if (token != null) {
        LaunchedShare(token = token, onShared = viewModel::clearInviteToken)
    }

    val editState = state.editState

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editState != null) stringResource(R.string.cd_edit_list)
                        else state.list?.name ?: stringResource(R.string.detail_list_fallback),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (editState != null) viewModel.cancelEdit() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (editState == null) {
                        val exportChooserTitle = stringResource(R.string.cd_export_chooser)
                        OverflowMenu(
                            viewModel,
                            onNewCategory = { showNewCategory = true },
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
                        )
                    }
                },
            )
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
                    androidx.compose.material3.Button(
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
            } else {
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
                state.rows.isEmpty() -> Text(
                    stringResource(R.string.cd_empty),
                    Modifier.align(Alignment.Center).padding(32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
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
                                onToggle = { viewModel.toggleItem(row.item) },
                                onEdit = { editingItem = row.item },
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
        com.dcapps.spese.ui.components.ConfirmDialog(
            title = stringResource(R.string.editor_delete_item),
            message = stringResource(R.string.cd_delete_item_message, item.name),
            onConfirm = { viewModel.deleteItem(item) },
            onDismiss = { itemToDelete = null },
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
        com.dcapps.spese.ui.components.ConfirmDialog(
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
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cd_import_title)) },
        text = {
            androidx.compose.foundation.layout.Column {
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
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onImport(text) }) { Text(stringResource(R.string.cd_import_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun LaunchedShare(token: String, onShared: () -> Unit) {
    val context = LocalContext.current
    val link = "https://spese-dc.web.app/checklist/join/$token"
    val shareText = stringResource(R.string.cd_share_text, link)
    val chooserTitle = stringResource(R.string.detail_share_chooser)
    androidx.compose.runtime.LaunchedEffect(token) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
        onShared()
    }
}

@Composable
private fun OverflowMenu(
    viewModel: ChecklistDetailViewModel,
    onNewCategory: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.detail_more_actions))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_edit)) },
            onClick = { expanded = false; viewModel.enterEditMode() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.editor_new_category)) },
            onClick = { expanded = false; onNewCategory() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.cd_menu_check_all)) },
            onClick = { expanded = false; viewModel.checkAll(true) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.cd_menu_uncheck_all)) },
            onClick = { expanded = false; viewModel.checkAll(false) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.cd_menu_collapse_all)) },
            onClick = { expanded = false; viewModel.setAllCollapsed(true) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.cd_menu_expand_all)) },
            onClick = { expanded = false; viewModel.setAllCollapsed(false) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.detail_share)) },
            onClick = { expanded = false; viewModel.generateInviteToken() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.cd_menu_export)) },
            onClick = { expanded = false; onExport() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.cd_import_confirm)) },
            onClick = { expanded = false; onImport() },
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (8 + row.depth * 16).dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleCollapse) {
            Icon(
                imageVector = if (row.collapsed) {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = if (row.collapsed) stringResource(R.string.cd_expand) else stringResource(R.string.cd_collapse),
            )
        }
        // Tap sul nome = collassa/espande (come la freccetta).
        Text(
            text = row.category.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).clickable { onToggleCollapse() }.padding(vertical = 8.dp),
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit_category))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.editor_delete_category))
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun ItemRow(
    row: ChecklistRow.Item,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    val item = row.item
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(start = (8 + row.depth * 16).dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(checked = item.checked, onCheckedChange = { onToggle() })
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
    }
}

@Composable
private fun NewCategoryDialog(
    categories: List<com.dcapps.spese.core.model.ShoppingCategory>,
    onDismiss: () -> Unit,
    onCreate: (name: String, parentId: Long?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<Long?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_new_category)) },
        text = {
            androidx.compose.foundation.layout.Column {
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
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name, parentId) }) { Text(stringResource(R.string.action_create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ParentCategoryPicker(
    categories: List<com.dcapps.spese.core.model.ShoppingCategory>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val noneLabel = stringResource(R.string.cd_parent_none)
    val options = listOf<Pair<Long?, String>>(null to noneLabel) +
        categories.map { it.id to it.name }
    val label = options.firstOrNull { it.first == selectedId }?.second ?: noneLabel

    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.cd_parent_category)) },
            trailingIcon = {
                androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.fillMaxWidth()
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cd_category_title)) },
        text = {
            androidx.compose.foundation.layout.Column {
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
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name, parentId) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) } },
    )
}
