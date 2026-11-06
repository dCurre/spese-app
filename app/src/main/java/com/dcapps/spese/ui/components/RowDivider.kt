package com.dcapps.spese.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dcapps.spese.ui.theme.SpeseTheme

/**
 * Divisore standard tra righe di lista: colore [SpeseTheme.colors.divider] + padding orizzontale.
 * Punto unico, al posto delle tante copie inline di `HorizontalDivider(..., color = divider)`.
 */
@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        color = SpeseTheme.colors.divider,
    )
}
