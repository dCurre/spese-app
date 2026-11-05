package com.dcapps.spese.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Preferenza di tema dell'utente, allineata al campo `theme_preference` del backend
 * ("light" | "dark" | "auto" | "legacy").
 * - LIGHT: stile "Ledger verde" chiaro.
 * - DARK: scuro material attuale (invariato).
 * - LEGACY: scuro-verde (stile Ledger su fondo scuro).
 */
enum class ThemePreference { LIGHT, DARK, AUTO, LEGACY }

/**
 * Colori semantici dell'app (saldi/importi, accenti), oltre al colorScheme Material.
 * Accessibili nei composable via [LocalSpeseColors] / `MaterialTheme`-like helper.
 */
data class SpeseSemanticColors(
    val credit: Color,
    val debit: Color,
    val accent: Color,
    val divider: Color,
)

val LocalSpeseColors = staticCompositionLocalOf {
    SpeseSemanticColors(
        credit = CreditLight,
        debit = DebitLight,
        accent = AccentAmberLight,
        divider = LightDivider,
    )
}

/** Accesso comodo ai colori semantici: `SpeseTheme.colors.credit`. */
object SpeseTheme {
    val colors: SpeseSemanticColors
        @Composable get() = LocalSpeseColors.current
}

// Tema scuro material attuale (invariato).
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

// Tema chiaro "Ledger verde".
private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenDark,
    secondary = BrandGreen,
    background = LightBg,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightSub,
    outline = LightDivider,
)

// Tema "legacy": scuro-verde.
private val LegacyColorScheme = darkColorScheme(
    primary = LegacyAccent,
    onPrimary = Color(0xFF0E1512),
    primaryContainer = LegacyAccentDark,
    secondary = LegacyAccent,
    background = LegacyBgPage,
    onBackground = LegacyTextPrimary,
    surface = LegacyBgCard,
    onSurface = LegacyTextPrimary,
    surfaceVariant = LegacyBgInput,
    onSurfaceVariant = LegacyTextSecondary,
    outline = LegacyBorder,
)

@Composable
fun SpeseTheme(
    preference: ThemePreference = ThemePreference.AUTO,
    // Material You: disattivato per coerenza cromatica tra dispositivi.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (preference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.LEGACY -> true
        ThemePreference.AUTO -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        preference == ThemePreference.LEGACY -> LegacyColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Colori semantici coerenti col tema (scuro = toni più brillanti).
    val semantic = if (darkTheme) {
        SpeseSemanticColors(
            credit = CreditDark,
            debit = DebitDark,
            accent = AccentAmberDark,
            divider = if (preference == ThemePreference.LEGACY) LegacyBorder else Color(0xFF2A2A2A),
        )
    } else {
        SpeseSemanticColors(
            credit = CreditLight,
            debit = DebitLight,
            accent = AccentAmberLight,
            divider = LightDivider,
        )
    }

    CompositionLocalProvider(LocalSpeseColors provides semantic) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
