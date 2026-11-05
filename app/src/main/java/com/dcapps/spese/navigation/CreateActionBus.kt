package com.dcapps.spese.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter

/**
 * Canale di eventi "crea" (one-shot) tra il pulsante "+" centrale della bottom bar e la schermata
 * top-level attualmente visibile. La bottom bar vive nel NavHost, ma l'azione di creazione (e il
 * relativo dialog) appartiene alla singola schermata: il "+" emette un evento per la rotta attiva,
 * la schermata lo colleziona e apre il proprio dialog di creazione.
 */
object CreateActionBus {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** Richiede l'azione "crea" per la rotta [route] (es. Routes.EXPENSES_LIST). */
    fun request(route: String) {
        _events.tryEmit(route)
    }
}

/** La schermata della rotta [route] reagisce all'evento "crea" del "+" della bottom bar. */
@Composable
fun OnCreateRequested(route: String, onCreate: () -> Unit) {
    LaunchedEffect(route) {
        CreateActionBus.events.filter { it == route }.collect { onCreate() }
    }
}
