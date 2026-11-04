package com.dcapps.spese.core.bugreport

import android.content.Context
import java.io.File

/**
 * Persiste lo stacktrace dell'ultimo crash su file, così al riavvio successivo l'app può
 * proporre all'utente di inviare la segnalazione.
 */
object CrashStore {
    private const val FILE_NAME = "last_crash.txt"

    fun save(context: Context, throwable: Throwable) {
        runCatching {
            File(context.filesDir, FILE_NAME).writeText(throwable.stackTraceToString())
        }
    }

    /** Ritorna lo stacktrace dell'ultimo crash non ancora gestito, o null. */
    fun read(context: Context): String? {
        val f = File(context.filesDir, FILE_NAME)
        return if (f.exists()) runCatching { f.readText() }.getOrNull() else null
    }

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
    }
}
