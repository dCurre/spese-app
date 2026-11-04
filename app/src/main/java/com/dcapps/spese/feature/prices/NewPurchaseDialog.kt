package com.dcapps.spese.feature.prices

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.dcapps.spese.core.model.MeasurementUnit
import com.dcapps.spese.core.model.Purchase
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPurchaseDialog(
    defaultUnitId: Long?,
    units: List<MeasurementUnit>,
    purchase: Purchase? = null,
    onDismiss: () -> Unit,
    onConfirm: (price: Double, quantity: Double, unitId: Long, purchasedAt: String, store: String?) -> Unit,
) {
    var priceText by remember { mutableStateOf(purchase?.price?.toString() ?: "") }
    var qtyText by remember { mutableStateOf(purchase?.quantity?.toString() ?: "1") }
    var store by remember { mutableStateOf(purchase?.store ?: "") }
    var unitId by remember {
        mutableStateOf(purchase?.unitId ?: defaultUnitId ?: units.firstOrNull()?.id)
    }
    var expanded by remember { mutableStateOf(false) }

    val price = priceText.replace(",", ".").toDoubleOrNull()
    val qty = qtyText.replace(",", ".").toDoubleOrNull()
    val valid = price != null && price > 0 && qty != null && qty > 0 && unitId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (purchase == null) "Nuovo acquisto" else "Modifica acquisto") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Prezzo (€)") },
                    singleLine = true,
                    isError = priceText.isNotBlank() && (price == null || price <= 0),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Quantità") },
                    singleLine = true,
                    isError = qtyText.isNotBlank() && (qty == null || qty <= 0),
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
                            label = { Text("Unità") },
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
                OutlinedTextField(
                    value = store,
                    onValueChange = { store = it },
                    label = { Text("Negozio (opzionale)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val date = purchase?.purchasedAt
                        ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    onConfirm(price!!, qty!!, unitId!!, date, store)
                },
            ) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
