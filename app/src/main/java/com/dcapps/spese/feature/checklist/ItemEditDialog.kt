package com.dcapps.spese.feature.checklist

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import com.dcapps.spese.core.model.ShoppingCategory
import com.dcapps.spese.core.model.ShoppingItem
import com.dcapps.spese.ui.components.ReadOnlyPickerField
import com.dcapps.spese.ui.components.SpeseDialog
import com.dcapps.spese.ui.theme.SpeseTheme

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

    SpeseDialog(
        title = stringResource(R.string.item_edit_title),
        onDismiss = onDismiss,
        // Elimina come icona cestino (danger) accanto al titolo; i bottoni in basso restano
        // lo standard Salva/Annulla.
        titleAction = {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = SpeseTheme.colors.danger,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = qtyText,
                onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.item_quantity)) },
                singleLine = true,
                isError = qtyText.isNotBlank() && (qty == null || qty <= 0),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            val noCategoryLabel = stringResource(R.string.item_no_category)
            ReadOnlyPickerField(
                label = stringResource(R.string.item_category),
                options = buildList<Pair<Long?, String>> {
                    add(null to noCategoryLabel)
                    categories.forEach { add(it.id to it.name) }
                },
                selectedId = categoryId,
                onSelect = { categoryId = it },
            )
        },
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onConfirm(name, qty ?: 1, categoryId) },
        confirmEnabled = valid,
    )
}

