package com.dcapps.spese.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Preferenza di tema dell'utente, allineata al campo `theme_preference` del backend
 * ("light" | "dark" | "auto" | "legacy").
 * - DEFAULT: tema predefinito, scuro-verde "Ledger".
 * - LIGHT: stile "Ledger verde" chiaro.
 * - DARK: scuro material.
 * - LEGACY: navy del frontend webapp.
 * - AUTO: segue il tema di sistema (light/dark).
 */
enum class ThemePreference { DEFAULT, LIGHT, DARK, AUTO, LEGACY }

/**
 * Colori semantici dell'app (oltre al ColorScheme Material), accessibili via `SpeseTheme.colors`.
 * Derivano dagli stessi [ThemeTokens] del tema attivo → sempre coerenti col tema.
 */
data class SpeseSemanticColors(
    val credit: Color,
    val debit: Color,
    val accent: Color,
    val divider: Color,
    val badgeBg: Color,
)

val LocalSpeseColors = staticCompositionLocalOf { LightTokens.toSemanticColors() }

/** Accesso comodo ai colori semantici: `SpeseTheme.colors.credit`. */
object SpeseTheme {
    val colors: SpeseSemanticColors
        @Composable get() = LocalSpeseColors.current
}

/**
 * Builder UNICO token→ColorScheme Material: la mappatura "cosa = quale token" è scritta una sola
 * volta qui; cambiano solo i valori dei token per tema.
 */
private fun ThemeTokens.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = brand,
        onPrimary = onBrand,
        primaryContainer = brandDark,
        secondary = brand,
        background = bgPage,
        onBackground = textPrimary,
        surface = bgCard,
        onSurface = textPrimary,
        surfaceVariant = bgInput,
        onSurfaceVariant = textSecondary,
        outline = border,
    )
}

/** Builder UNICO token→colori semantici. */
private fun ThemeTokens.toSemanticColors() = SpeseSemanticColors(
    credit = credit,
    debit = debit,
    accent = accent,
    divider = border,
    badgeBg = bgInput,
)

@Composable
fun SpeseTheme(
    preference: ThemePreference = ThemePreference.AUTO,
    content: @Composable () -> Unit,
) {
    val dark = when (preference) {
        ThemePreference.DEFAULT -> true
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.LEGACY -> true
        ThemePreference.AUTO -> isSystemInDarkTheme()
    }

    // Scelta della "colonna" di valori in base al tema.
    val tokens = when (preference) {
        ThemePreference.DEFAULT -> DefaultTokens
        ThemePreference.LIGHT -> LightTokens
        ThemePreference.DARK -> DarkTokens
        ThemePreference.LEGACY -> LegacyTokens
        ThemePreference.AUTO -> if (dark) DarkTokens else LightTokens
    }

    CompositionLocalProvider(LocalSpeseColors provides tokens.toSemanticColors()) {
        MaterialTheme(
            colorScheme = tokens.toColorScheme(dark),
            typography = Typography,
            content = content,
        )
    }
}
