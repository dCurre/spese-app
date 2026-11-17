package com.dcapps.spese.core.update

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Worker periodico che controlla su GitHub Releases se c'è una versione più recente e, in tal caso,
 * invia una notifica di sistema (anche ad app chiusa). Complementa il controllo in-app all'avvio:
 * quello avvisa chi apre l'app, questo chi la tiene chiusa. Nessun FCM/backend: interroga la stessa
 * API pubblica del controllo in-app. Best-effort: in caso di rete/errore ritenta.
 */
@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val updateRepository: UpdateRepository,
    private val updateNotifier: UpdateNotifier,
    private val notifiedStore: UpdateNotifiedStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Senza il permesso/canale notifiche non ha senso procedere: niente da fare finché l'utente
        // non concede le notifiche (il controllo in-app resta comunque attivo).
        if (!updateNotifier.canNotify()) return Result.success()

        val info = runCatching { updateRepository.checkForUpdate() }
            .getOrElse { return Result.retry() }
            ?: return Result.success() // nessun aggiornamento

        // Una notifica per versione: se abbiamo già notificato questa, non ripetere.
        if (notifiedStore.lastNotified() == info.version) return Result.success()

        updateNotifier.notifyUpdate(info.version)
        notifiedStore.setLastNotified(info.version)
        return Result.success()
    }

    companion object {
        private const val NAME = "spese_update_check"

        /** Registra (una volta) il controllo aggiornamenti periodico: ogni 12h, solo con rete. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.KEEP, request,
            )
        }
    }
}
