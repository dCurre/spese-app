package com.dcapps.spese.feature.expenses

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
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.model.ExpenseType
import com.dcapps.spese.core.model.ExpensesListParticipant
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
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onConfirm: (name: String, amount: BigDecimal, ownerId: Long, date: Instant, typeId: Long?) -> Unit,
) {
    var name by remember { mutableStateOf(expense?.name ?: "") }
    var amountText by remember {
        mutableStateOf(expense?.amount?.takeIf { it.signum() != 0 }?.toPlainString() ?: "")
    }
    var selectedOwnerId by remember {
        mutableStateOf(expense?.owner?.id ?: participants.firstOrNull()?.userId)
    }
    var selectedTypeId by remember {
        mutableStateOf(expense?.expenseTypeId ?: expenseTypes.firstOrNull()?.id)
    }

    val amount = amountText.replace(",", ".").toBigDecimalOrNull()
    val valid = name.isNotBlank() &&
        amount != null && amount.signum() > 0 &&
        selectedOwnerId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense == null) "Nuova spesa" else "Modifica spesa") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Importo (€)") },
                    singleLine = true,
                    isError = amountText.isNotBlank() && (amount == null || amount.signum() <= 0),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                PickerField(
                    label = "Chi ha pagato",
                    options = participants.map { it.userId to participantLabel(it) },
                    selectedId = selectedOwnerId,
                    onSelect = { selectedOwnerId = it },
                )

                if (expenseTypes.isNotEmpty()) {
                    PickerField(
                        label = "Tipologia",
                        options = expenseTypes.map { it.id to it.name },
                        selectedId = selectedTypeId,
                        onSelect = { selectedTypeId = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onConfirm(
                        name,
                        amount!!,
                        selectedOwnerId!!,
                        expense?.expenseDate ?: Instant.now(),
                        selectedTypeId,
                    )
                },
            ) { Text("Salva") }
        },
        dismissButton = {
            if (expense != null && onDelete != null) {
                TextButton(onClick = onDelete) { Text("Elimina") }
            } else {
                TextButton(onClick = onDismiss) { Text("Annulla") }
            }
        },
    )
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

private fun participantLabel(p: ExpensesListParticipant): String =
    listOfNotNull(p.name, p.surname).joinToString(" ").ifBlank { p.email ?: "Utente ${p.userId}" }
