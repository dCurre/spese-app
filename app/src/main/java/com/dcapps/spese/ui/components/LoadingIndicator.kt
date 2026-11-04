package com.dcapps.spese.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Indicatore di caricamento che, se l'attesa si prolunga, mostra un avviso sul cold start del
 * backend (Render mette in pausa il servizio dopo inattività e il primo accesso è lento).
 */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    var slow by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        delay(4000)
        slow = true
    }
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        if (slow) {
            Text(
                "Il server si sta avviando, potrebbe volerci qualche secondo…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, start = 32.dp, end = 32.dp),
            )
        }
    }
}
