package com.dcapps.spese.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import retrofit2.HttpException
import java.io.IOException

/** Esito di una chiamata di rete, con messaggio di errore già estratto dal corpo. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(
        val message: String,
        val code: Int? = null,
        val cause: Throwable? = null,
    ) : ApiResult<Nothing>
}

/**
 * Esegue [block] convertendo eccezioni in [ApiResult.Error]. Estrae il messaggio dal corpo
 * `{"error": ...}` del backend quando presente.
 */
suspend fun <T> apiCall(json: Json, block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        // Corpo di risposta del backend (es. {"error": "..."}): utile sia per il messaggio che
        // per la segnalazione, perché lo stacktrace è offuscato da R8.
        val rawBody = e.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() }
        val request = e.response()?.raw()?.request
        ApiResult.Error(
            message = parseErrorMessage(json, rawBody) ?: "Errore ${e.code()}",
            code = e.code(),
            cause = e,
        ).also {
            // Gli errori applicativi ATTESI (permessi / validazione) sono risposte legittime del
            // backend, non bug: mostrali all'utente ma NON segnalarli come crash-report.
            if (!isExpectedAppError(e.code())) {
                com.dcapps.spese.core.bugreport.ErrorReporterHolder.instance?.record(
                    message = it.message,
                    cause = e,
                    httpMethod = request?.method,
                    url = request?.url?.encodedPath,
                    httpCode = e.code(),
                    serverBody = rawBody,
                )
            }
        }
    } catch (e: IOException) {
        ApiResult.Error(
            message = "Impossibile contattare il server. Controlla la connessione.",
            cause = e,
        ).also { it.report() }
    } catch (e: Exception) {
        ApiResult.Error(message = e.message ?: "Errore imprevisto", cause = e).also { it.report() }
    }

private fun ApiResult.Error.report() {
    com.dcapps.spese.core.bugreport.ErrorReporterHolder.instance
        ?.record(message, cause)
}

/**
 * Codici HTTP che rappresentano errori applicativi ATTESI (dipendono da permessi/input
 * dell'utente, non da un bug): vanno mostrati all'utente ma non segnalati come crash-report.
 *  - 400 bad request / 422 validazione
 *  - 403 permessi insufficienti (es. cambio ruolo non autorizzato)
 *  - 409 conflitto
 */
private fun isExpectedAppError(code: Int): Boolean = code in setOf(400, 403, 409, 422)

/** Estrae il messaggio dal corpo `{"error": ...}` già letto; [rawBody] può essere null. */
private fun parseErrorMessage(json: Json, rawBody: String?): String? {
    val body = rawBody ?: return null
    return try {
        val obj = json.parseToJsonElement(body).jsonObject
        when (val error = obj["error"]) {
            is JsonPrimitive -> error.content
            is JsonObject -> error.values.joinToString("; ") { it.toString().trim('[', ']', '"') }
            else -> body
        }
    } catch (_: Exception) {
        body
    }
}

