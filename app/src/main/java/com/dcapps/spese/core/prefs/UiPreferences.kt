package com.dcapps.spese.core.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferenze locali di UI (SharedPreferences), sul modello di [SortPreferences].
 *
 * Al momento contiene solo [showPendingSyncBadges]: il flag che mostra/nasconde l'indicatore
 * "in attesa di sincronizzazione" sulle singole righe (liste/articoli/spese/prezzi). È una
 * preferenza di device (vale anche per il guest, che non ha account backend), esposta come
 * [StateFlow] così la UI reagisce al cambiamento del toggle in Impostazioni.
 */
@Singleton
class UiPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)

    private val _showPendingSyncBadges = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_PENDING_SYNC, true),
    )

    /** true = mostra il badge "in attesa di sync" per-riga (default). */
    val showPendingSyncBadges: StateFlow<Boolean> = _showPendingSyncBadges.asStateFlow()

    fun setShowPendingSyncBadges(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_PENDING_SYNC, enabled).apply()
        _showPendingSyncBadges.value = enabled
    }

    private companion object {
        const val KEY_SHOW_PENDING_SYNC = "show_pending_sync_badges"
    }
}
