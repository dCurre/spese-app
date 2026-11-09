package com.dcapps.spese.core.sync

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
 * Worker periodico che esegue il sync di tutte le sezioni local-first (checklist, spese, prezzi) in
 * background (anche ad app chiusa), quando c'è rete. È uno dei trigger del SyncEngine, insieme a:
 * dopo-modifica (debounced) e avvio/foreground. In guest ogni engine è no-op.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val checklistSync: ChecklistSyncEngine,
    private val expensesSync: ExpensesSyncEngine,
    private val pricesSync: PricesSyncEngine,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        checklistSync.sync()
        expensesSync.sync()
        pricesSync.sync()
        return Result.success()
    }

    companion object {
        private const val NAME = "spese_periodic_sync"

        /** Registra (una volta) il sync periodico: ogni 15 min (minimo WorkManager), solo con rete. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
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
