package com.dcapps.spese.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Preferenza di tema dell'utente, allineata al campo `theme_preference` del backend
 * ("light" | "dark" | "auto" | "legacy"). "legacy" riproduce i colori della webapp.
 */
enum class ThemePreference { LIGHT, DARK, AUTO, LEGACY }

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

/** Tema "legacy": palette scura blu-navy con accent verde, come la webapp. */
private val LegacyColorScheme = darkColorScheme(
    primary = LegacyAccent,
    onPrimary = Color.White,
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

    // Un unico MaterialTheme: cambia solo il colorScheme, così il passaggio tra temi è
    // istantaneo e non rimonta l'albero (niente "ricaricamento" dell'app).
    val colorScheme = when {
        preference == ThemePreference.LEGACY -> LegacyColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
