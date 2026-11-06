package com.dcapps.spese.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Bottom sheet dell'app: pannello che sale dal basso. La chiusura con tap fuori, tasto Indietro e
 * swipe verso il basso è gestita nativamente da [ModalBottomSheet]. Punto unico per il look dei
 * bottom sheet (coerenza + futuro restyling).
 *
 * @param onDismiss invocato alla richiesta di chiusura (tap-fuori/back/swipe-giù o azione interna).
 * @param title titolo opzionale mostrato in cima.
 * @param sheetState stato dello sheet; di default parte espanso.
 * @param content corpo libero del pannello.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeseBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            content()
        }
    }
}

/**
 * Bottom sheet "a form": titolo, contenuto e in fondo i bottoni Annulla / Conferma, con gli stessi
 * colori di [SpeseDialog] (conferma verde o rossa se distruttiva, annulla neutro). Rimpiazzo diretto
 * di [SpeseDialog] quando si vuole il pannello dal basso invece del dialog centrale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeseFormBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    dismissText: String = androidx.compose.ui.res.stringResource(com.dcapps.spese.R.string.action_cancel),
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable () -> Unit,
) {
    SpeseBottomSheet(
        onDismiss = onDismiss,
        title = title,
        sheetState = sheetState,
    ) {
        content()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            androidx.compose.material3.TextButton(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) { Text(dismissText) }
            SpeseConfirmButton(
                text = confirmText,
                onClick = onConfirm,
                enabled = confirmEnabled,
                destructive = destructive,
            )
        }
    }
}
