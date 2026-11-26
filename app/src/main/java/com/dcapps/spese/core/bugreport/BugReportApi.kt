package com.dcapps.spese.core.bugreport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/** Body per creare una issue su GitHub. */
@Serializable
data class CreateIssueRequest(
    val title: String,
    val body: String,
    val labels: List<String> = listOf("bug"),
)

@Serializable
data class CreateIssueResponse(
    val number: Int,
    @SerialName("html_url") val htmlUrl: String,
)

/** API GitHub per creare issue di segnalazione bug (richiede token con permesso Issues). */
interface BugReportApi {
    /** La issue GitHub la crea il backend: nell'app non c'è alcun token. */
    @POST("api/bug-reports")
    suspend fun createIssue(@Body body: CreateIssueRequest): CreateIssueResponse
}
