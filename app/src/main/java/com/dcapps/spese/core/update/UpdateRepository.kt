package com.dcapps.spese.core.update

import com.dcapps.spese.BuildConfig
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Informazioni su un aggiornamento disponibile. */
data class UpdateInfo(
    val version: String,
    val changelog: String,
    val apkUrl: String,
    val apkSize: Long,
    /** URL della pagina della release su GitHub (changelog completo), se disponibile. */
    val releaseUrl: String? = null,
)

@Singleton
class UpdateRepository @Inject constructor(
    private val githubApi: GithubApi,
) {
    private val owner = "dCurre"
    private val repo = "spese-app"

    /**
     * Ritorna [UpdateInfo] se su GitHub c'è una release con versione maggiore di quella
     * installata e con un asset APK; altrimenti null (nessun aggiornamento o nessun APK).
     * Non lancia: in caso di errore di rete ritorna null (l'updater è best-effort).
     */
    suspend fun checkForUpdate(): UpdateInfo? {
        val current = SemVer.parse(BuildConfig.VERSION_NAME) ?: return null
        val release = runCatching { githubApi.latestRelease(owner, repo) }.getOrNull() ?: return null
        if (release.draft || release.prerelease) return null

        val latest = SemVer.parse(release.tagName) ?: return null
        if (latest <= current) return null

        val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?: return null

        return UpdateInfo(
            version = latest.toString(),
            changelog = release.body?.takeIf { it.isNotBlank() } ?: (release.name ?: ""),
            apkUrl = apk.browserDownloadUrl,
            apkSize = apk.size,
            releaseUrl = release.htmlUrl,
        )
    }
}
