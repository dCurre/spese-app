package com.dcapps.spese.core.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistenza locale (SharedPreferences) della preferenza di ordinamento per ciascuna lista, così
 * l'ordinamento scelto dall'utente viene ricordato tra una sessione e l'altra. (issue #9)
 *
 * Ogni schermata usa una [key] stabile (es. "prices", "expenses", "checklist_lists",
 * "checklist_items"). Salviamo il nome dell'enum del criterio + la direzione.
 */
@Singleton
class SortPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("sort_prefs", Context.MODE_PRIVATE)

    /** Ordinamento salvato per [key], o null se mai impostato. */
    fun get(key: String): Saved? {
        val criterion = prefs.getString("$key.criterion", null) ?: return null
        val ascending = prefs.getBoolean("$key.asc", true)
        return Saved(criterion, ascending)
    }

    /** Salva criterio (nome enum) e direzione per [key]. */
    fun set(key: String, criterion: String, ascending: Boolean) {
        prefs.edit()
            .putString("$key.criterion", criterion)
            .putBoolean("$key.asc", ascending)
            .apply()
    }

    data class Saved(val criterion: String, val ascending: Boolean)

    companion object Keys {
        const val PRICES = "prices"
        const val EXPENSES = "expenses"
        const val CHECKLIST_LISTS = "checklist_lists"
        const val CHECKLIST_ITEMS = "checklist_items"
    }
}
