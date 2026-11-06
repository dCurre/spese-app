package com.dcapps.spese.core.feedback

import android.os.Build
import com.dcapps.spese.BuildConfig
import com.dcapps.spese.core.bugreport.BugReportApi
import com.dcapps.spese.core.bugreport.CreateIssueRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.apiCall
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Invia i feedback / richieste di feature come issue su GitHub (`dCurre/spese-app`), con
 * titolo `[Feature]` e label `enhancement` per distinguerle dai bug (stesso endpoint e token
 * del bug-report, che ha permesso Issues:write). A differenza del bug-report NON allega
 * stacktrace: solo la descrizione dell'utente + la versione app (utile per sapere su quale
 * versione è stata chiesta la feature).
 */
@Singleton
class FeedbackRepository @Inject constructor(
    private val api: BugReportApi,
    private val json: Json,
) {
    private val owner = "dCurre"
    private val repo = "spese-app"

    val isConfigured: Boolean get() = BuildConfig.GITHUB_BUGREPORT_TOKEN.isNotBlank()

    /** Crea una issue di tipo feature-request. Ritorna l'URL della issue creata. */
    suspend fun report(userDescription: String): ApiResult<String> = apiCall(json) {
        val title = buildString {
            append("[Feature] ")
            val firstLine = userDescription.lineSequence().firstOrNull()?.take(80)?.takeIf { it.isNotBlank() }
            append(firstLine ?: "Richiesta funzionalità")
        }
        val body = buildString {
            appendLine("### Richiesta")
            appendLine(userDescription.ifBlank { "(nessuna descrizione)" })
            appendLine()
            appendLine("### Ambiente")
            appendLine("- App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("- Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("- Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}")
        }
        val resp = api.createIssue(
            auth = "Bearer ${BuildConfig.GITHUB_BUGREPORT_TOKEN}",
            owner = owner,
            repo = repo,
            body = CreateIssueRequest(title = title, body = body, labels = listOf("enhancement")),
        )
        resp.htmlUrl
    }
}
