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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.model.ExpenseType
import com.dcapps.spese.core.model.ExpensesListParticipant
import com.dcapps.spese.core.util.toShortDate
import com.dcapps.spese.ui.components.AutocompleteField
import com.dcapps.spese.ui.components.DateField
import com.dcapps.spese.ui.components.ReadOnlyPickerField
import com.dcapps.spese.ui.components.SpeseDialog
import com.dcapps.spese.ui.theme.SpeseTheme
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

    val amount = amountText.replace(",", ".").toBigDecimalOrNull()
    val valid = name.isNotBlank() &&
        amount != null && amount.signum() > 0 &&
        selectedOwnerId != null

    SpeseDialog(
        title = if (isEdit) stringResource(R.string.expense_edit_title)
            else stringResource(R.string.detail_new_expense),
        onDismiss = onDismiss,
        icon = Icons.Outlined.Receipt,
        // In modifica: elimina in basso a sinistra (danger), separato da Annulla/Salva a destra.
        onDelete = if (isEdit) onDelete else null,
        content = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // Nome con autocomplete dai nomi di spese già presenti.
                AutocompleteField(
                    value = name,
                    onValueChange = { name = it },
                    suggestions = nameSuggestions,
                    label = stringResource(R.string.name),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.expense_amount)) },
                    singleLine = true,
                    isError = amountText.isNotBlank() && (amount == null || amount.signum() <= 0),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                // "Chi ha pagato" solo per le liste condivise: in quelle personali è sempre
                // il proprietario, quindi il campo sarebbe superfluo.
                if (!isPersonal) {
                    ReadOnlyPickerField(
                        label = stringResource(R.string.expense_who_paid),
                        options = participants.map { it.userId to participantLabel(it) },
                        selectedId = selectedOwnerId,
                        onSelect = { selectedOwnerId = it },
                    )
                }

                if (expenseTypes.isNotEmpty()) {
                    ReadOnlyPickerField(
                        label = stringResource(R.string.expense_type),
                        options = expenseTypes.map { it.id to it.name },
                        selectedId = selectedTypeId,
                        onSelect = { selectedTypeId = it },
                    )
                }

                DateField(
                    label = stringResource(R.string.expense_date),
                    displayText = selectedDate.toShortDate(),
                    selectedUtcMillis = selectedDate.toEpochMilli(),
                    onDateSelected = { selectedDate = Instant.ofEpochMilli(it) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onConfirm(name, amount!!, selectedOwnerId!!, selectedDate, selectedTypeId) },
        confirmEnabled = valid,
    )
}

internal fun participantLabel(p: ExpensesListParticipant): String =
    listOfNotNull(p.name, p.surname).joinToString(" ").ifBlank { p.email ?: "Utente ${p.userId}" }
