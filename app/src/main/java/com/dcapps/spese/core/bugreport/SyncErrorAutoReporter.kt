package com.dcapps.spese.core.bugreport

import android.content.Context
import com.dcapps.spese.BuildConfig
import com.dcapps.spese.core.network.ApiResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Invia AUTOMATICAMENTE una segnalazione su GitHub quando il sync fallisce con un errore REALE
 * (non una semplice mancanza di rete — quella è già esclusa da `runSyncStep`).
 *
 * Anti-spam: una sola segnalazione per "firma" dell'errore (versione app + codice HTTP + metodo +
 * endpoint + messaggio + corpo server normalizzato) entro una finestra di [COOLDOWN_MS]. Errori con
 * causa diversa sullo stesso endpoint → firme diverse → segnalati entrambi; una nuova versione
 * dell'app azzera il dedup. La firma e il timestamp dell'ultimo invio sono
 * persistiti in SharedPreferences, così il cooldown vale anche tra riavvii dell'app (il sync gira
 * ogni ~15 min: senza questo si aprirebbero decine di issue identiche).
 *
 * È silenzioso: nessun avviso all'utente, l'unico segnale resta il badge rosso.
 */
@Singleton
class SyncErrorAutoReporter @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: BugReportRepository,
) {
    private val prefs = context.getSharedPreferences("sync_autoreport", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    /**
     * Valuta se segnalare l'errore e, in caso, lo invia in background. Best-effort: non lancia mai.
     * [label] è il passo di sync (es. "checklist push") — entra nella descrizione.
     */
    fun maybeReport(label: String, error: ReportableError) {
        if (!repository.isConfigured) return
        scope.launch {
            mutex.withLock {
                val signature = signatureOf(error)
                val now = System.currentTimeMillis()
                val lastAt = prefs.getLong(keyTime(signature), 0L)
                if (now - lastAt < COOLDOWN_MS) return@withLock
                // Marca PRIMA dell'invio: se la rete fallisce di nuovo non riproviamo subito in loop.
                prefs.edit().putLong(keyTime(signature), now).apply()
                val result = repository.report(
                    userDescription = "Segnalazione automatica: sincronizzazione non riuscita ($label).",
                    stackTrace = error.stackTrace,
                    error = error,
                )
                // Se l'invio è fallito, azzera il marcatore così si ritenterà al prossimo errore.
                if (result is ApiResult.Error) prefs.edit().remove(keyTime(signature)).apply()
            }
        }
    }

    /**
     * Firma stabile dell'errore: stessa causa → stessa firma → una sola issue nel cooldown.
     *
     * Include il [ReportableError.serverBody] NORMALIZZATO: così due errori sullo stesso endpoint
     * con stesso codice HTTP ma causa diversa (es. "owner_id mancante" vs "Errore interno") hanno
     * firme diverse e vengono segnalati entrambi. La normalizzazione azzera le cifre (id, numeri,
     * timestamp) così "lista 5 non trovata" e "lista 9 non trovata" restano UNA sola firma, evitando
     * lo spam di issue che differiscono solo per un id.
     *
     * Include anche la VERSIONE dell'app (`VERSION_NAME`): dopo un rilascio il dedup riparte da capo,
     * così se lo stesso errore ricompare in una build nuova viene riaperta una issue invece di
     * restare silenziato dal cooldown della versione precedente.
     */
    private fun signatureOf(e: ReportableError): String {
        val raw = listOf(
            BuildConfig.VERSION_NAME,
            e.httpCode?.toString().orEmpty(),
            e.httpMethod.orEmpty(),
            e.url.orEmpty(),
            e.message,
            normalize(e.serverBody.orEmpty()),
        ).joinToString("|")
        return raw.hashCode().toString()
    }

    /** Toglie le cifre (id/numeri/timestamp variabili) così errori uguali-salvo-id condividono la firma. */
    private fun normalize(s: String): String = s.replace(Regex("\\d+"), "#")

    private fun keyTime(signature: String) = "ts_$signature"

    private companion object {
        // 12 ore: un errore persistente genera al massimo 2 issue al giorno.
        const val COOLDOWN_MS = 12 * 60 * 60 * 1000L
    }
}
