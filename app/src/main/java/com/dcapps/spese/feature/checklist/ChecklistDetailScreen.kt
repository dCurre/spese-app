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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    var showNewCategory by remember { mutableStateOf(false) }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    com.dcapps.spese.ui.components.MessageSnackbarEffect(
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
                title = { Text(if (editState != null) "Modifica lista" else state.list?.name ?: "Lista") },
                navigationIcon = {
                    IconButton(onClick = { if (editState != null) viewModel.cancelEdit() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    if (editState == null) {
                        OverflowMenu(viewModel, onNewCategory = { showNewCategory = true })
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
                        Text("Annulla")
                    }
                    androidx.compose.material3.Button(
                        onClick = viewModel::saveEdits,
                        enabled = !state.saving,
                        modifier = Modifier.weight(1f),
                    ) { Text(if (state.saving) "Salvataggio…" else "Salva") }
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
                        placeholder = { Text("Aggiungi articolo") },
                        singleLine = true,
                    )
                    IconButton(
                        onClick = { viewModel.addItem(newItem, null); newItem = "" },
                        enabled = newItem.isNotBlank(),
                    ) { Icon(Icons.Default.Add, contentDescription = "Aggiungi") }
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
                    "Lista vuota. Aggiungi un articolo qui sotto.",
                    Modifier.align(Alignment.Center).padding(32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.rows, key = { rowKey(it) }) { row ->
                        when (row) {
                            is ChecklistRow.CategoryHeader -> CategoryHeaderRow(
                                row = row,
                                onClick = { editingCategory = row.category },
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
            onDelete = { viewModel.deleteItem(item); editingItem = null },
            onConfirm = { name, qty, catId ->
                viewModel.updateItem(item, name, qty, catId)
                editingItem = null
            },
        )
    }

    editingCategory?.let { cat ->
        CategoryEditDialog(
            category = cat,
            onDismiss = { editingCategory = null },
            onDelete = { viewModel.deleteCategory(cat); editingCategory = null },
            onRename = { name -> viewModel.renameCategory(cat, name); editingCategory = null },
        )
    }

    if (showNewCategory) {
        NewCategoryDialog(
            onDismiss = { showNewCategory = false },
            onCreate = { name -> viewModel.addCategory(name, null); showNewCategory = false },
        )
    }
}

@Composable
private fun LaunchedShare(token: String, onShared: () -> Unit) {
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(token) {
        val link = "https://spese-dc.web.app/checklist/join/$token"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Unisciti alla mia checklist: $link")
        }
        context.startActivity(Intent.createChooser(intent, "Condividi lista"))
        onShared()
    }
}

@Composable
private fun OverflowMenu(viewModel: ChecklistDetailViewModel, onNewCategory: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "Altre azioni")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Modifica") },
            onClick = { expanded = false; viewModel.enterEditMode() },
        )
        DropdownMenuItem(
            text = { Text("Nuova categoria") },
            onClick = { expanded = false; onNewCategory() },
        )
        DropdownMenuItem(
            text = { Text("Spunta tutto") },
            onClick = { expanded = false; viewModel.checkAll(true) },
        )
        DropdownMenuItem(
            text = { Text("Despunta tutto") },
            onClick = { expanded = false; viewModel.checkAll(false) },
        )
        DropdownMenuItem(
            text = { Text("Condividi") },
            onClick = { expanded = false; viewModel.generateInviteToken() },
        )
    }
}

private fun rowKey(row: ChecklistRow): String = when (row) {
    is ChecklistRow.CategoryHeader -> "c${row.category.id}"
    is ChecklistRow.Item -> "i${row.item.id}"
}

@Composable
private fun CategoryHeaderRow(row: ChecklistRow.CategoryHeader, onClick: () -> Unit) {
    Text(
        text = row.category.name,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = (16 + row.depth * 16).dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
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
private fun NewCategoryDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova categoria") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name) }) { Text("Crea") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun CategoryEditDialog(
    category: ShoppingCategory,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember { mutableStateOf(category.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Categoria") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onRename(name) }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDelete) { Text("Elimina") } },
    )
}
