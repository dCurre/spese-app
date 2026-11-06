package com.dcapps.spese.feature.prices

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
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
import com.dcapps.spese.core.model.MeasurementUnit
import com.dcapps.spese.ui.components.SpeseDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProductDialog(
    units: List<MeasurementUnit>,
    product: com.dcapps.spese.core.model.Product? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, brand: String?, unitId: Long?) -> Unit,
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var brand by remember { mutableStateOf(product?.brand ?: "") }
    var unitId by remember { mutableStateOf(product?.defaultUnitId ?: units.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }

    SpeseDialog(
        title = if (product == null) stringResource(R.string.prices_new_product)
            else stringResource(R.string.product_edit_title),
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text(stringResource(R.string.product_brand)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (units.isNotEmpty()) {
                val selected = units.firstOrNull { it.id == unitId }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    OutlinedTextField(
                        value = selected?.let { "${it.name} (${it.symbol})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.product_unit)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        units.forEach { u ->
                            DropdownMenuItem(
                                text = { Text("${u.name} (${u.symbol})") },
                                onClick = { unitId = u.id; expanded = false },
                            )
                        }
                    }
                }
            }
        },
        confirmText = if (product == null) stringResource(R.string.action_create)
            else stringResource(R.string.action_save),
        onConfirm = { onConfirm(name, brand, unitId) },
        confirmEnabled = name.isNotBlank(),
    )
}
