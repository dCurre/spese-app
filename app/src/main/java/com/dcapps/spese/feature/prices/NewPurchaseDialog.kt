package com.dcapps.spese.feature.prices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import com.dcapps.spese.core.model.Purchase
import com.dcapps.spese.ui.components.SpeseDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
    // Data dell'acquisto (yyyy-MM-dd): in modifica quella esistente, altrimenti oggi.
    val isoFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    var purchaseDate by remember {
        mutableStateOf(
            purchase?.purchasedAt?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
                ?: LocalDate.now(),
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }

    // L'unità è quella dell'acquisto (in modifica) o quella del prodotto.
    val unitId = purchase?.unitId ?: productUnitId
    val unitSymbol = purchase?.unitSymbol ?: productUnitSymbol

    val price = priceText.replace(",", ".").toDoubleOrNull()
    val qty = qtyText.replace(",", ".").toDoubleOrNull()
    val valid = price != null && price > 0 && qty != null && qty > 0 && unitId != null

    SpeseDialog(
        title = if (purchase == null) stringResource(R.string.purchase_new_title)
            else stringResource(R.string.purchase_edit_title),
        onDismiss = onDismiss,
        content = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(stringResource(R.string.purchase_price)) },
                    singleLine = true,
                    isError = priceText.isNotBlank() && (price == null || price <= 0),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = {
                        Text(
                            if (unitSymbol != null) stringResource(R.string.purchase_quantity_unit, unitSymbol)
                            else stringResource(R.string.purchase_quantity),
                        )
                    },
                    singleLine = true,
                    isError = qtyText.isNotBlank() && (qty == null || qty <= 0),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (unitId == null) {
                    Text(
                        stringResource(R.string.purchase_no_unit),
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
                        label = { Text(stringResource(R.string.purchase_store)) },
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
                // Data acquisto: in sola lettura, apre il date picker al tocco (un Box trasparente
                // sopra il campo intercetta il click, come nel dialog spesa).
                Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = purchaseDate.format(isoFmt),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.expense_date)) },
                        trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        Modifier.matchParentSize().clickable { showDatePicker = true },
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.purchase_notes)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onConfirm(price!!, qty!!, unitId!!, purchaseDate.format(isoFmt), store, notes) },
        confirmEnabled = valid,
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = purchaseDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        purchaseDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
