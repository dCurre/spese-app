package com.dcapps.spese

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dcapps.spese.core.bugreport.CrashStore
import com.dcapps.spese.core.bugreport.ErrorReporter
import com.dcapps.spese.core.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SpeseApplication : Application(), Configuration.Provider {

    // Iniettato per popolare ErrorReporterHolder all'avvio (usato da apiCall).
    @Inject lateinit var errorReporter: ErrorReporter

    // Factory di WorkManager integrata con Hilt (per iniettare le dipendenze nei Worker).
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
        SyncWorker.schedule(this) // sync periodico in background (solo con rete)
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
