package com.dcapps.spese.feature.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.unit.dp
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.model.ExpenseType
import com.dcapps.spese.core.model.ExpensesListParticipant
import com.dcapps.spese.core.util.toShortDate
import java.math.BigDecimal
import java.time.Instant

/**
 * Dialog di creazione/modifica spesa: nome, importo (>0), chi ha pagato (partecipante),
 * tipologia. La data è impostata a ora corrente (il date picker arriverà con la UI completa).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDialog(
    expense: Expense?,
    participants: List<ExpensesListParticipant>,
    expenseTypes: List<ExpenseType>,
    // In una lista personale non si chiede "chi ha pagato": è sempre il proprietario.
    isPersonal: Boolean = false,
    personalOwnerId: Long? = null,
    // Nomi di spese già presenti, per l'autocomplete del campo nome.
    nameSuggestions: List<String> = emptyList(),
    // Se true, il dialog è in modalità "crea" anche se [expense] è valorizzato (usato da "Ripeti").
    forceCreate: Boolean = false,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onConfirm: (name: String, amount: BigDecimal, ownerId: Long, date: Instant, typeId: Long?) -> Unit,
) {
    val isEdit = expense != null && !forceCreate
    var name by remember { mutableStateOf(expense?.name ?: "") }
    var nameMenuExpanded by remember { mutableStateOf(false) }
    var amountText by remember {
        mutableStateOf(expense?.amount?.takeIf { it.signum() != 0 }?.toPlainString() ?: "")
    }
    var selectedOwnerId by remember {
        mutableStateOf(
            expense?.owner?.id
                ?: personalOwnerId.takeIf { isPersonal }
                ?: participants.firstOrNull()?.userId,
        )
    }
    var selectedTypeId by remember {
        mutableStateOf(expense?.expenseTypeId ?: expenseTypes.firstOrNull()?.id)
    }
    var selectedDate by remember {
        mutableStateOf(expense?.expenseDate ?: Instant.now())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val amount = amountText.replace(",", ".").toBigDecimalOrNull()
    val valid = name.isNotBlank() &&
        amount != null && amount.signum() > 0 &&
        selectedOwnerId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Modifica spesa" else "Nuova spesa") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // Nome con autocomplete dai nomi di spese già presenti.
                val nameMatches = nameSuggestions.filter {
                    name.isNotBlank() && it.contains(name, ignoreCase = true) &&
                        !it.equals(name, ignoreCase = true)
                }.distinct().take(5)
                ExposedDropdownMenuBox(
                    expanded = nameMenuExpanded && nameMatches.isNotEmpty(),
                    onExpandedChange = { nameMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameMenuExpanded = true },
                        label = { Text("Nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                    )
                    DropdownMenu(
                        expanded = nameMenuExpanded && nameMatches.isNotEmpty(),
                        onDismissRequest = { nameMenuExpanded = false },
                    ) {
                        nameMatches.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = { name = s; nameMenuExpanded = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Importo (€)") },
                    singleLine = true,
                    isError = amountText.isNotBlank() && (amount == null || amount.signum() <= 0),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                // "Chi ha pagato" solo per le liste condivise: in quelle personali è sempre
                // il proprietario, quindi il campo sarebbe superfluo.
                if (!isPersonal) {
                    PickerField(
                        label = "Chi ha pagato",
                        options = participants.map { it.userId to participantLabel(it) },
                        selectedId = selectedOwnerId,
                        onSelect = { selectedOwnerId = it },
                    )
                }

                if (expenseTypes.isNotEmpty()) {
                    PickerField(
                        label = "Tipologia",
                        options = expenseTypes.map { it.id to it.name },
                        selectedId = selectedTypeId,
                        onSelect = { selectedTypeId = it },
                    )
                }

                // Campo data: in sola lettura, apre il date picker al tocco.
                // Il click arriva da un Box trasparente sovrapposto (un TextField readOnly
                // non emette eventi di click sul corpo).
                Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = selectedDate.toShortDate(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data") },
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onConfirm(name, amount!!, selectedOwnerId!!, selectedDate, selectedTypeId)
                },
            ) { Text("Salva") }
        },
        dismissButton = {
            if (isEdit && onDelete != null) {
                TextButton(onClick = onDelete) { Text("Elimina") }
            } else {
                TextButton(onClick = onDismiss) { Text("Annulla") }
            }
        },
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { selectedDate = Instant.ofEpochMilli(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerField(
    label: String,
    options: List<Pair<Long, String>>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { onSelect(id); expanded = false },
                )
            }
        }
    }
}

internal fun participantLabel(p: ExpensesListParticipant): String =
    listOfNotNull(p.name, p.surname).joinToString(" ").ifBlank { p.email ?: "Utente ${p.userId}" }
