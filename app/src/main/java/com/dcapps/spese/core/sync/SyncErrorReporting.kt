package com.dcapps.spese.core.sync

import com.dcapps.spese.core.bugreport.ErrorReporter
import com.dcapps.spese.core.bugreport.ReportableError
import com.dcapps.spese.core.bugreport.SyncErrorAutoReporter
import com.dcapps.spese.core.network.isExpectedAppError
import retrofit2.HttpException
import java.io.IOException

/**
 * Esegue un passo di sync (push/pull) in modo BEST-EFFORT: non rilancia mai, così un fallimento di
 * una sezione non blocca le altre né fa crashare il worker.
 *
 * Reporting degli errori (per l'azione "Segnala" delle snackbar):
 * - [IOException] (offline / timeout): SILENZIATA. Il sync gira anche in background ogni 15 min; un
 *   errore di rete transitorio non è un bug e non deve generare rumore.
 * - [HttpException] con codice applicativo ATTESO (400/403/409/422): non è un bug → non segnalata.
 * - Ogni altro errore (5xx, contratto rotto, eccezioni impreviste): registrato in [ErrorReporter],
 *   così appena l'utente apre una snackbar di errore l'azione "Segnala" ha già i dettagli.
 *
 * Nota: è l'equivalente, per il sync, di ciò che `apiCall` fa per le chiamate dei repository.
 *
 * Se [autoReporter] è passato, un errore REALE viene anche segnalato automaticamente su GitHub
 * (con dedup + cooldown lato [SyncErrorAutoReporter]). Gli errori di rete e quelli applicativi
 * attesi non generano mai una segnalazione.
 *
 * @return true se il passo è fallito con un errore REALE (non una semplice mancanza di rete), così
 *   il chiamante può aggiornare l'indicatore di stato sync. IOException → false (non è un errore reale).
 */
internal suspend fun ErrorReporter.runSyncStep(
    label: String,
    autoReporter: SyncErrorAutoReporter? = null,
    block: suspend () -> Unit,
): Boolean {
    return try {
        block()
        false
    } catch (e: IOException) {
        // Offline/timeout: atteso, nessuna segnalazione e nessuno stato di errore.
        false
    } catch (e: HttpException) {
        if (!isExpectedAppError(e.code())) {
            val request = e.response()?.raw()?.request
            val rawBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val message = "Sync $label: HTTP ${e.code()}"
            record(
                message = message,
                cause = e,
                httpMethod = request?.method,
                url = request?.url?.encodedPath,
                httpCode = e.code(),
                serverBody = rawBody?.takeIf { it.isNotBlank() },
            )
            autoReporter?.maybeReport(
                label,
                ReportableError(
                    message = message,
                    stackTrace = e.stackTraceToString(),
                    httpMethod = request?.method,
                    url = request?.url?.encodedPath,
                    httpCode = e.code(),
                    serverBody = rawBody?.takeIf { it.isNotBlank() },
                ),
            )
            true
        } else {
            // Errore applicativo atteso (permessi/validazione): non è un bug né uno stato di errore.
            false
        }
    } catch (e: Exception) {
        val message = "Sync $label: ${e.message ?: e.javaClass.simpleName}"
        record(message = message, cause = e)
        autoReporter?.maybeReport(label, ReportableError(message = message, stackTrace = e.stackTraceToString()))
        true
    }
}
