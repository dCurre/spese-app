package com.dcapps.spese.core.bugreport

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dettagli di un errore da poter segnalare. Per gli errori di rete sono valorizzati anche
 * [httpMethod]/[url]/[httpCode]/[serverBody]: sono le info più utili per il backend, dato che
 * lo stacktrace è offuscato da R8.
 */
data class ReportableError(
    val message: String,
    val stackTrace: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val httpMethod: String? = null,
    val url: String? = null,
    val httpCode: Int? = null,
    val serverBody: String? = null,
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

    fun record(
        message: String,
        cause: Throwable?,
        httpMethod: String? = null,
        url: String? = null,
        httpCode: Int? = null,
        serverBody: String? = null,
    ) {
        lastError = ReportableError(
            message = message,
            stackTrace = cause?.stackTraceToString(),
            httpMethod = httpMethod,
            url = url,
            httpCode = httpCode,
            serverBody = serverBody,
        )
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
