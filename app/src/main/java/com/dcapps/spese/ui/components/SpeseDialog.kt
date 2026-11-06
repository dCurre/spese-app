package com.dcapps.spese.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import com.dcapps.spese.ui.theme.SpeseTheme

/**
 * Dialog standard dell'app: un solo punto per bordi, contrasto e colori dei bottoni, così tutte
 * le modali sono coerenti (risolve le incoerenze tra le ~15 modali sparse).
 *
 * Colori bottoni (coerenti ovunque):
 *  - conferma non distruttiva → verde `primary`;
 *  - conferma distruttiva ([destructive] = true) → rosso `danger`;
 *  - annulla/dismiss → neutro.
 *
 * Per i casi particolari (es. un terzo bottone, o un dismiss che non è "Annulla") si possono
 * passare direttamente [confirmButton]/[dismissButton] al posto dei parametri testuali.
 */
@Composable
fun SpeseDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit = {},
    // Icona contestuale opzionale: se valorizzata, l'header mostra un cerchio colorato con l'icona
    // in cima e il titolo centrato sotto (stile "icona + colore tematico").
    icon: ImageVector? = null,
    iconTint: Color? = null,
    // Azione opzionale accanto al titolo (es. cestino distruttivo). Allineata a destra.
    // Ignorata quando è presente [icon] (header centrato).
    titleAction: (@Composable () -> Unit)? = null,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    dismissText: String = stringResource(R.string.action_cancel),
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    // Colore dell'icona: esplicito, oppure rosso se distruttiva, altrimenti accento primario.
    val tint = iconTint ?: if (destructive) SpeseTheme.colors.danger else MaterialTheme.colorScheme.primary
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = if (icon != null) {
            {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
                }
            }
        } else null,
        title = {
            when {
                // Con icona: titolo centrato sotto l'icona. Se c'è anche una titleAction (es. il
                // cestino di eliminazione), la si sovrappone a destra senza spostare il titolo.
                icon != null -> Box(Modifier.fillMaxWidth()) {
                    Text(
                        title,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    if (titleAction != null) {
                        Box(Modifier.align(Alignment.CenterEnd)) { titleAction() }
                    }
                }
                titleAction == null -> Text(title)
                else -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, modifier = Modifier.weight(1f))
                    titleAction()
                }
            }
        },
        text = { Column { content() } },
        confirmButton = {
            when {
                confirmButton != null -> confirmButton()
                confirmText != null && onConfirm != null -> SpeseConfirmButton(
                    text = confirmText,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    destructive = destructive,
                )
            }
        },
        dismissButton = {
            when {
                dismissButton != null -> dismissButton()
                // Annulla/dismiss NEUTRO (grigio), non verde: il verde è riservato alla conferma.
                else -> TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) { Text(dismissText) }
            }
        },
        // Bordo più contrastato dei default Material (migliora leggibilità del contorno modale).
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    )
}

/** Bottone di conferma standard: verde (primary) o rosso (danger) se distruttivo. */
@Composable
fun SpeseConfirmButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (destructive) SpeseTheme.colors.danger
            else MaterialTheme.colorScheme.primary,
        ),
    ) { Text(text) }
}

/** Bordo riutilizzabile per contornare le modali in modo più contrastato, se serve. */
@Composable
fun dialogBorder(): BorderStroke = BorderStroke(1.dp, SpeseTheme.colors.divider)
