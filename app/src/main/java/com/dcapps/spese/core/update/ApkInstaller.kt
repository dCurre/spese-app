package com.dcapps.spese.core.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadState {
    data class Progress(val percent: Int) : DownloadState
    data class Done(val file: File) : DownloadState
    data class Failed(val reason: String) : DownloadState
}

/**
 * Scarica l'APK di aggiornamento con [DownloadManager] e lancia l'installer di sistema.
 * Il file viene salvato nella cache esterna privata dell'app e condiviso via FileProvider.
 */
@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Scarica l'APK da [url] riportando il progresso. Emette [DownloadState.Done] col file
     * scaricato quando completa. Sospende fino a fine download.
     */
    suspend fun download(url: String, onProgress: (Int) -> Unit): DownloadState {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "spese-update.apk"
        val destFile = File(context.externalCacheDir, fileName)
        if (destFile.exists()) destFile.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Aggiornamento Spese")
            .setDestinationUri(Uri.fromFile(destFile))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setMimeType("application/vnd.android.package-archive")

        val id = dm.enqueue(request)

        while (true) {
            val (status, percent, reason) = queryDownload(dm, id)
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> return DownloadState.Done(destFile)
                DownloadManager.STATUS_FAILED -> return DownloadState.Failed("Download fallito (codice $reason)")
                else -> {
                    onProgress(percent)
                    delay(400)
                }
            }
        }
    }

    private fun queryDownload(dm: DownloadManager, id: Long): Triple<Int, Int, Int> {
        dm.query(DownloadManager.Query().setFilterById(id)).use { cursor: Cursor ->
            if (!cursor.moveToFirst()) return Triple(DownloadManager.STATUS_FAILED, 0, -1)
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            val downloaded = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            )
            val total = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            )
            val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
            return Triple(status, percent, reason)
        }
    }

    /** Lancia l'installer di sistema per il [apk] scaricato. */
    fun install(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    /** True se l'app può installare pacchetti (API 26+ richiede un permesso per-app). */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()
}
