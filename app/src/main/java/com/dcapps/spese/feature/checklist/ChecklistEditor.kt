package com.dcapps.spese.feature.checklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import com.dcapps.spese.ui.theme.SpeseTheme

/**
 * Editor della checklist in modalità modifica: tutte le modifiche sono locali ([onEdit])
 * e verranno inviate in blocco con batch-save al salvataggio.
 */
@Composable
fun ChecklistEditor(
    editState: ChecklistEditState,
    onEdit: ((ChecklistEditState) -> ChecklistEditState) -> Unit,
) {
    var newItemName by remember { mutableStateOf("") }
    var newCatName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Aggiunta rapida categoria.
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.editor_new_category)) },
                    singleLine = true,
                )
                TextButton(
                    enabled = newCatName.isNotBlank(),
                    onClick = { onEdit { it.addCategory(newCatName, null) }; newCatName = "" },
                ) { Text(stringResource(R.string.action_add)) }
            }
        }

        items(editState.categories, key = { "cat${it.id}" }) { cat ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = cat.name,
                    onValueChange = { n -> onEdit { it.updateCategory(cat.id) { c -> c.copy(name = n) } } },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.item_category)) },
                    singleLine = true,
                )
                IconButton(onClick = { onEdit { it.removeCategory(cat.id) } }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.editor_delete_category), tint = SpeseTheme.colors.danger)
                }
            }
        }

        item {
            Text(
                stringResource(R.string.editor_items),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        items(editState.items, key = { "item${it.id}" }) { item ->
            EditItemRow(
                item = item,
                categories = editState.categories,
                onNameChange = { n -> onEdit { it.updateItem(item.id) { i -> i.copy(name = n) } } },
                onQtyChange = { q -> onEdit { it.updateItem(item.id) { i -> i.copy(quantity = q) } } },
                onCategoryChange = { c -> onEdit { it.updateItem(item.id) { i -> i.copy(categoryId = c) } } },
                onDelete = { onEdit { it.removeItem(item.id) } },
            )
        }

        // Aggiunta rapida articolo.
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.editor_new_item)) },
                    singleLine = true,
                )
                IconButton(
                    enabled = newItemName.isNotBlank(),
                    onClick = { onEdit { it.addItem(newItemName, null) }; newItemName = "" },
                ) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.editor_add_item)) }
            }
        }
    }
}

@Composable
private fun EditItemRow(
    item: EditItem,
    categories: List<EditCategory>,
    onNameChange: (String) -> Unit,
    onQtyChange: (Int) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = item.name,
                onValueChange = onNameChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = item.quantity.toString(),
                onValueChange = { onQtyChange(it.toIntOrNull()?.coerceAtLeast(1) ?: 1) },
                modifier = Modifier.width(72.dp).padding(start = 8.dp),
                label = { Text(stringResource(R.string.editor_qty_short)) },
                singleLine = true,
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.editor_delete_item), tint = SpeseTheme.colors.danger)
            }
        }
        CategoryDropdown(
            categories = categories,
            selectedId = item.categoryId,
            onSelect = onCategoryChange,
        )
    }
}

@Composable
private fun CategoryDropdown(
    categories: List<EditCategory>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val noCategoryLabel = stringResource(R.string.item_no_category)
    val selectedName = categories.firstOrNull { it.id == selectedId }?.name ?: noCategoryLabel

    OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        Text(selectedName)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(noCategoryLabel) },
            onClick = { onSelect(null); expanded = false },
        )
        categories.forEach { c ->
            DropdownMenuItem(
                text = { Text(c.name) },
                onClick = { onSelect(c.id); expanded = false },
            )
        }
    }
}
