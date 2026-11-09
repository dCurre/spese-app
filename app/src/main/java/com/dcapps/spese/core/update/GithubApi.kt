package com.dcapps.spese.core.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

/** Release di GitHub (sottoinsieme dei campi che ci servono). */
@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0,
)

/** API pubblica di GitHub per leggere l'ultima release del repo. */
interface GithubApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): GithubRelease

    /** Scarica un file grezzo (es. CHANGELOG.md da raw.githubusercontent.com) via URL assoluto. */
    @GET
    suspend fun rawFile(@Url url: String): ResponseBody
}
