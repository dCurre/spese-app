package com.dcapps.spese.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.dcapps.spese.R

/**
 * Campo data in sola lettura che apre un DatePicker al tocco. Incapsula il pattern (campo
 * readOnly + Box trasparente per il click + DatePickerDialog con OK/Annulla) ripetuto nei dialog.
 *
 * Lavora in millis UTC: [selectedUtcMillis] è il valore mostrato, [onDateSelected] riporta il
 * nuovo valore scelto (millis UTC a inizio giornata). La formattazione dell'etichetta è lasciata
 * al chiamante via [displayText], così ognuno usa il proprio tipo/formato (Instant, LocalDate…).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    displayText: String,
    selectedUtcMillis: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(modifier) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Un TextField readOnly non emette click sul corpo: lo intercetta un Box trasparente sopra.
        Box(Modifier.matchParentSize().clickable { showPicker = true })
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedUtcMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let(onDateSelected)
                    showPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
