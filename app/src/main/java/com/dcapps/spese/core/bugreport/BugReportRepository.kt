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
    ): ApiResult<String> = apiCall(json) {
        val title = buildString {
            append("[Bug] ")
            append(userDescription.lineSequence().firstOrNull()?.take(80)?.ifBlank { "Segnalazione" } ?: "Segnalazione")
        }
        val body = buildString {
            appendLine("### Descrizione")
            appendLine(userDescription.ifBlank { "(nessuna descrizione)" })
            appendLine()
            appendLine("### Ambiente")
            appendLine("- App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("- Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("- Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}")
            if (!stackTrace.isNullOrBlank()) {
                appendLine()
                appendLine("### Stacktrace")
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
}
