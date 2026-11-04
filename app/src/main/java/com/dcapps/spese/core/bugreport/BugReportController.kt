package com.dcapps.spese.core.bugreport

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordina l'apertura del dialog di segnalazione bug da qualunque schermata.
 * Una schermata chiama [request] (es. dal tap "Segnala" di una snackbar); MainActivity
 * osserva [visible] e mostra il dialog.
 */
@Singleton
class BugReportController @Inject constructor() {

    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    fun request() { _visible.value = true }
    fun dismiss() { _visible.value = false }
}
