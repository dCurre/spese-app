package com.dcapps.spese.feature.prices

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import com.dcapps.spese.core.model.Purchase
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Dialog nuovo/modifica acquisto. L'unità di misura NON si sceglie qui: è quella del prodotto
 * ([productUnitId]); viene solo mostrata come riferimento e inviata automaticamente.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NewPurchaseDialog(
    productUnitId: Long?,
    productUnitSymbol: String?,
    purchase: Purchase? = null,
    storeSuggestions: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (price: Double, quantity: Double, unitId: Long, purchasedAt: String, store: String?, notes: String?) -> Unit,
) {
    var priceText by remember { mutableStateOf(purchase?.price?.toString() ?: "") }
    var qtyText by remember { mutableStateOf(purchase?.quantity?.toString() ?: "1") }
    var store by remember { mutableStateOf(purchase?.store ?: "") }
    var notes by remember { mutableStateOf(purchase?.notes ?: "") }
    var storeMenuExpanded by remember { mutableStateOf(false) }

    // L'unità è quella dell'acquisto (in modifica) o quella del prodotto.
    val unitId = purchase?.unitId ?: productUnitId
    val unitSymbol = purchase?.unitSymbol ?: productUnitSymbol

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
                    label = { Text(if (unitSymbol != null) "Quantità ($unitSymbol)" else "Quantità") },
                    singleLine = true,
                    isError = qtyText.isNotBlank() && (qty == null || qty <= 0),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (unitId == null) {
                    Text(
                        "Questo prodotto non ha un'unità di misura. Impostala modificando il prodotto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                // Negozio con autocomplete dai negozi già usati.
                val matches = storeSuggestions.filter {
                    store.isNotBlank() && it.contains(store, ignoreCase = true) &&
                        !it.equals(store, ignoreCase = true)
                }.take(5)
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = storeMenuExpanded && matches.isNotEmpty(),
                    onExpandedChange = { storeMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    OutlinedTextField(
                        value = store,
                        onValueChange = { store = it; storeMenuExpanded = true },
                        label = { Text("Negozio (opzionale)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable),
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = storeMenuExpanded && matches.isNotEmpty(),
                        onDismissRequest = { storeMenuExpanded = false },
                    ) {
                        matches.forEach { s ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(s) },
                                onClick = { store = s; storeMenuExpanded = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note (opzionale)") },
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
                    onConfirm(price!!, qty!!, unitId!!, date, store, notes)
                },
            ) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
