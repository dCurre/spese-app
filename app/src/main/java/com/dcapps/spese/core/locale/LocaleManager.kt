package com.dcapps.spese.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Gestione della lingua dell'app (per-app locale). Usa AppCompatDelegate, che su Android 13+
 * si appoggia al sistema e sotto gestisce la persistenza internamente.
 */
object LocaleManager {

    /** Codice lingua corrente ("it", "en", …) o "" se segue il sistema. */
    fun currentLanguage(): String =
        AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')

    /** Imposta la lingua dell'app. [tag] es. "it" / "en"; vuoto = segui il sistema. */
    fun setLanguage(tag: String) {
        val locales = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
