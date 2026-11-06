package com.dcapps.spese.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Esegue [onResume] ogni volta che la schermata torna in primo piano (ON_RESUME), incluso il
 * ritorno da una schermata di dettaglio via back.
 *
 * Serve a mantenere allineate le schermate elenco quando si modifica il CONTENUTO di un elemento
 * nel dettaglio (es. spuntare/cancellare articoli di una checklist, aggiungere acquisti a un
 * prodotto): quelle modifiche cambiano i conteggi mostrati nell'elenco ma NON toccano la riga
 * della lista stessa, quindi il realtime sulla tabella delle liste non scatta. Un refresh al
 * ritorno risolve senza dipendere dagli eventi realtime.
 */
@Composable
fun RefreshOnResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume by rememberUpdatedState(onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
