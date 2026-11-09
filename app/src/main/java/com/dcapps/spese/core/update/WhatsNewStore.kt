package com.dcapps.spese.core.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ricorda l'ultima versione per cui sono già state mostrate le novità ("what's new" post-update),
 * così al primo avvio su una versione nuova mostriamo le novità una sola volta. (SharedPreferences)
 */
@Singleton
class WhatsNewStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("whats_new", Context.MODE_PRIVATE)

    /** Ultima versione per cui le novità sono già state mostrate, o null se mai (primo install). */
    fun lastShown(): String? = prefs.getString(KEY, null)

    fun setLastShown(version: String) = prefs.edit().putString(KEY, version).apply()

    private companion object {
        const val KEY = "last_shown_version"
    }
}
