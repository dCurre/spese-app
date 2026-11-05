package com.dcapps.spese.core.bugreport

import android.os.Build
import com.dcapps.spese.BuildConfig
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.apiCall
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BugReportRepository @Inject constructor(
    private val api: BugReportApi,
    private val json: Json,
) {
    private val owner = "dCurre"
    private val repo = "spese-app"

    val isConfigured: Boolean get() = BuildConfig.GITHUB_BUGREPORT_TOKEN.isNotBlank()

    /**
     * Crea una issue su GitHub con la descrizione dell'utente, lo stacktrace e le info
     * di versione/dispositivo. Ritorna l'URL della issue creata.
     */
    suspend fun report(
        userDescription: String,
        stackTrace: String?,
        error: ReportableError? = null,
    ): ApiResult<String> = apiCall(json) {
        // Riepilogo rete (es. "500 DELETE /api/products/12") se disponibile: è l'informazione
        // più utile per il backend, dato che lo stacktrace è offuscato da R8.
        val netSummary = error?.let { e ->
            listOfNotNull(e.httpCode?.toString(), e.httpMethod, e.url)
                .joinToString(" ")
                .takeIf { it.isNotBlank() }
        }
        val title = buildString {
            append("[Bug] ")
            val descFirstLine = userDescription.lineSequence().firstOrNull()?.take(80)?.takeIf { it.isNotBlank() }
            append(descFirstLine ?: netSummary ?: "Segnalazione")
        }
        val body = buildString {
            appendLine("### Descrizione")
            appendLine(userDescription.ifBlank { "(nessuna descrizione)" })
            appendLine()
            appendLine("### Ambiente")
            appendLine("- App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("- Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("- Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("- Quando: ${isoUtc(error?.timestamp ?: System.currentTimeMillis())}")

            if (error != null && (error.url != null || error.httpCode != null)) {
                appendLine()
                appendLine("### Richiesta fallita")
                error.httpCode?.let { appendLine("- Codice HTTP: $it") }
                if (error.httpMethod != null || error.url != null) {
                    appendLine("- Endpoint: ${listOfNotNull(error.httpMethod, error.url).joinToString(" ")}")
                }
                error.message.takeIf { it.isNotBlank() }?.let { appendLine("- Messaggio: $it") }
                error.serverBody?.takeIf { it.isNotBlank() }?.let {
                    appendLine()
                    appendLine("Risposta del server:")
                    appendLine("```json")
                    appendLine(it.take(2000))
                    appendLine("```")
                }
            }

            if (!stackTrace.isNullOrBlank()) {
                appendLine()
                appendLine("### Stacktrace (offuscato R8)")
                appendLine("```")
                appendLine(stackTrace.take(6000))
                appendLine("```")
            }
        }
        val resp = api.createIssue(
            auth = "Bearer ${BuildConfig.GITHUB_BUGREPORT_TOKEN}",
            owner = owner,
            repo = repo,
            body = CreateIssueRequest(title = title, body = body),
        )
        resp.htmlUrl
    }

    private fun isoUtc(epochMillis: Long): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return fmt.format(java.util.Date(epochMillis))
    }
}
