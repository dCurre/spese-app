package com.dcapps.spese.feature.checklist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dcapps.spese.core.model.ShoppingCategory
import com.dcapps.spese.core.model.ShoppingItem

/** Dialog di modifica articolo: nome, quantità e categoria. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditDialog(
    item: ShoppingItem,
    categories: List<ShoppingCategory>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: (name: String, quantity: Int, categoryId: Long?) -> Unit,
) {
    var name by remember { mutableStateOf(item.name) }
    var qtyText by remember { mutableStateOf((item.quantity ?: 1).toString()) }
    var categoryId by remember { mutableStateOf(item.categoryId) }

    val qty = qtyText.toIntOrNull()
    val valid = name.isNotBlank() && qty != null && qty > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica articolo") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                    label = { Text("Quantità") },
                    singleLine = true,
                    isError = qtyText.isNotBlank() && (qty == null || qty <= 0),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                CategoryPicker(
                    categories = categories,
                    selectedId = categoryId,
                    onSelect = { categoryId = it },
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(name, qty ?: 1, categoryId) }) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) { Text("Elimina") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(
    categories: List<ShoppingCategory>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf<Pair<Long?, String>>(null to "(Nessuna categoria)") +
        categories.map { it.id to it.name }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: "(Nessuna categoria)"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoria") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { onSelect(id); expanded = false },
                )
            }
        }
    }
}
