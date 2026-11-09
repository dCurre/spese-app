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
    /** Novità per versione (dalla installata esclusa alla disponibile inclusa), più recenti prima. */
    val entries: List<ChangelogEntry> = emptyList(),
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

        // Changelog "parlante" dal CHANGELOG.md del repo: mostra le novità di TUTTE le versioni
        // dalla installata (esclusa) alla disponibile (inclusa). Best-effort: se il download
        // fallisce, resta la lista vuota (la UI ricade sul body della release).
        val entries = runCatching {
            val md = githubApi.rawFile(CHANGELOG_URL).string()
            Changelog.entriesInRange(md, from = current, to = latest)
        }.getOrDefault(emptyList())

        return UpdateInfo(
            version = latest.toString(),
            changelog = release.body?.takeIf { it.isNotBlank() } ?: (release.name ?: ""),
            apkUrl = apk.browserDownloadUrl,
            apkSize = apk.size,
            releaseUrl = release.htmlUrl,
            entries = entries,
        )
    }

    /**
     * Novità da mostrare DOPO un aggiornamento, al primo avvio sulla nuova versione: le sezioni del
     * CHANGELOG tra l'ultima versione già vista (esclusa) e quella installata (inclusa).
     * Ritorna lista vuota se: primo install (niente da mostrare), già viste, o download fallito.
     */
    suspend fun whatsNewSince(lastShownVersion: String?): List<ChangelogEntry> {
        val current = SemVer.parse(BuildConfig.VERSION_NAME) ?: return emptyList()
        // Primo install (nessuna versione vista prima): non mostrare nulla.
        val from = SemVer.parse(lastShownVersion) ?: return emptyList()
        if (current <= from) return emptyList()
        return runCatching {
            val md = githubApi.rawFile(CHANGELOG_URL).string()
            Changelog.entriesInRange(md, from = from, to = current)
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val CHANGELOG_URL =
            "https://raw.githubusercontent.com/dCurre/spese-app/main/CHANGELOG.md"
    }
}
