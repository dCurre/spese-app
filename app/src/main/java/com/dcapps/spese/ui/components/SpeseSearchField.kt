package com.dcapps.spese.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R

/**
 * Campo di ricerca standard, usato in tutte le liste. Punto unico, al posto delle copie identiche
 * sparse nelle schermate.
 *
 * Realizzato con [BasicTextField] + [OutlinedTextFieldDefaults.DecorationBox] (invece di
 * `OutlinedTextField`) per poter ridurre l'altezza: il contentPadding compatto e [heightIn] basso
 * rendono la barra più sottile verticalmente (issue #20) mantenendo bordo arrotondato, lente e
 * pulsante "X".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeseSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val textStyle = LocalTextStyle.current.merge(MaterialTheme.typography.bodyMedium)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.heightIn(min = 44.dp),
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = query,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = { Text(placeholder, style = textStyle) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear_search))
                        }
                    }
                } else null,
                // Padding verticale compatto: è ciò che riduce l'altezza complessiva del campo.
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        shape = RoundedCornerShape(24.dp),
                    )
                },
            )
        },
    )
}
