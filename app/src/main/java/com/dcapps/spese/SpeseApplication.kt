package com.dcapps.spese

import android.app.Application
import com.dcapps.spese.core.bugreport.CrashStore
import com.dcapps.spese.core.bugreport.ErrorReporter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SpeseApplication : Application() {

    // Iniettato per popolare ErrorReporterHolder all'avvio (usato da apiCall).
    @Inject lateinit var errorReporter: ErrorReporter

    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
    }

    /** Salva lo stacktrace dei crash non gestiti, così al riavvio si può proporre la segnalazione. */
    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { CrashStore.save(this, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
