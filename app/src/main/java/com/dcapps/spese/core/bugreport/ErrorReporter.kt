package com.dcapps.spese.core.bugreport

import javax.inject.Inject
import javax.inject.Singleton

/** Dettagli di un errore da poter segnalare. */
data class ReportableError(
    val message: String,
    val stackTrace: String?,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Tiene traccia dell'ultimo errore applicativo (non fatale) catturato, così la UI può
 * offrire "Segnala" senza che ogni ViewModel debba propagare lo stacktrace.
 */
@Singleton
class ErrorReporter @Inject constructor() {

    @Volatile
    var lastError: ReportableError? = null
        private set

    fun record(message: String, cause: Throwable?) {
        lastError = ReportableError(message = message, stackTrace = cause?.stackTraceToString())
    }

    fun clear() {
        lastError = null
    }

    init {
        ErrorReporterHolder.instance = this
    }
}

/**
 * Accesso statico all'[ErrorReporter] per `apiCall` (funzione top-level senza DI).
 * Popolato quando Hilt crea il singleton.
 */
object ErrorReporterHolder {
    @Volatile
    var instance: ErrorReporter? = null
}
