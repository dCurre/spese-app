package com.dcapps.spese.core.bugreport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

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
    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateIssueRequest,
    ): CreateIssueResponse
}
