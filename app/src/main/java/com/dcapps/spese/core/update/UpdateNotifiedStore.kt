package com.dcapps.spese.core.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ricorda l'ultima versione per cui è già stata inviata la notifica di sistema "aggiornamento
 * disponibile", così il worker periodico non ripete la notifica a ogni esecuzione (una notifica
 * per versione). Separato da [WhatsNewStore]: lì si ricorda cosa è già stato *mostrato dopo*
 * l'update, qui cosa è già stato *notificato prima*. (SharedPreferences)
 */
@Singleton
class UpdateNotifiedStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("update_notified", Context.MODE_PRIVATE)

    /** Ultima versione per cui è già stata inviata la notifica, o null se mai. */
    fun lastNotified(): String? = prefs.getString(KEY, null)

    fun setLastNotified(version: String) = prefs.edit().putString(KEY, version).apply()

    private companion object {
        const val KEY = "last_notified_version"
    }
}
