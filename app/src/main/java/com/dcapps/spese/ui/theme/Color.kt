package com.dcapps.spese.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * TOKEN SEMANTICI del tema: un nome per ogni "cosa a schermo".
 * La mappatura token→Material/semantico è UNICA (vedi Theme.kt); qui sotto ci sono solo i VALORI,
 * una "colonna" per tema (light / dark / legacy). Aggiungere un tema = riempire una colonna;
 * cambiare un colore = toccare un solo valore. (Stesso schema della lingua: stesse chiavi, valori diversi.)
 */
data class ThemeTokens(
    // superfici / sfondi
    val bgPage: Color,        // sfondo pagina
    val bgCard: Color,        // card / superfici
    val bgInput: Color,       // campi input / superfici elevate / badge
    val border: Color,        // bordi / divisori
    // testo
    val textPrimary: Color,
    val textSecondary: Color,
    // brand / primario
    val brand: Color,         // colore primario (bottoni, chip attivo, FAB)
    val brandDark: Color,     // variante scura del primario
    val onBrand: Color,       // testo/icone sopra il brand
    // semantici (importi/saldi, accenti)
    val credit: Color,        // positivo / credito
    val debit: Color,         // negativo / debito / spesa
    val accent: Color,        // accento (es. stella preferiti, seconda fetta grafico)
    // errore / azioni distruttive (cestino, Annulla/Cancel, sfondo swipe-delete).
    // UN solo colore: sfondo e "sopra" si derivano da questo nel builder (vedi Theme.kt).
    val danger: Color,
)

// --- LIGHT ("Ledger verde" chiaro) ---
val LightTokens = ThemeTokens(
    bgPage = Color(0xFFF5F7F6),
    bgCard = Color(0xFFFFFFFF),
    bgInput = Color(0xFFEDF1EF),
    border = Color(0xFFE6EBE8),
    textPrimary = Color(0xFF15241F),
    textSecondary = Color(0xFF6B7D76),
    brand = Color(0xFF1D9E75),
    brandDark = Color(0xFF157A5A),
    onBrand = Color(0xFFFFFFFF),
    credit = Color(0xFF1D9E75),
    debit = Color(0xFFD1495B),
    accent = Color(0xFFE8A13A),
    danger = Color(0xFFD1495B),
)

// --- DARK (scuro material attuale, viola — invariato come look) ---
val DarkTokens = ThemeTokens(
    bgPage = Color(0xFF1C1B1F),
    bgCard = Color(0xFF2B2930),
    bgInput = Color(0xFF36343B),
    border = Color(0xFF6B6673),  // più chiaro del fondo input per far risaltare i bordi dei campi
    textPrimary = Color(0xFFE6E1E5),
    textSecondary = Color(0xFFCAC4D0),
    brand = Color(0xFFD0BCFF),       // Purple80 (primario material scuro attuale)
    brandDark = Color(0xFF9A82DB),
    onBrand = Color(0xFF381E72),
    credit = Color(0xFF3FD39E),
    debit = Color(0xFFF4738A),
    accent = Color(0xFFE8A13A),
    danger = Color(0xFFF4738A),
)

// --- DEFAULT / "Predefinito" (scuro-verde "Ledger" su fondo scuro) ---
val DefaultTokens = ThemeTokens(
    bgPage = Color(0xFF0E1512),
    bgCard = Color(0xFF17201C),
    bgInput = Color(0xFF1E2A24),
    border = Color(0xFF3F5349),  // più chiaro del fondo input per far risaltare i bordi dei campi
    textPrimary = Color(0xFFEAF1EE),
    textSecondary = Color(0xFF8A9A93),
    brand = Color(0xFF2FB996),
    brandDark = Color(0xFF1D9E75),
    onBrand = Color(0xFF0E1512),
    credit = Color(0xFF3FD39E),
    debit = Color(0xFFF4738A),
    accent = Color(0xFFE8A13A),
    danger = Color(0xFFF4738A),
)

// --- LEGACY (navy del frontend webapp: dark blu + accent verde) ---
val LegacyTokens = ThemeTokens(
    bgPage = Color(0xFF0D1018),
    bgCard = Color(0xFF181E2E),
    bgInput = Color(0xFF1E2538),
    border = Color(0xFF424C6E),  // più chiaro del fondo input per far risaltare i bordi dei campi
    textPrimary = Color(0xFFDDE1EF),
    textSecondary = Color(0xFF7B849E),
    brand = Color(0xFF00B37E),
    brandDark = Color(0xFF009E6E),
    onBrand = Color(0xFF0D1018),
    credit = Color(0xFF00B37E),
    debit = Color(0xFFF4738A),
    accent = Color(0xFFE8A13A),
    danger = Color(0xFFF4738A),
)

// Brand verde (icona app) — usato anche fuori dai temi (es. splash/icona via @color).
val BrandGreen = Color(0xFF1D9E75)
val BrandGreenDark = Color(0xFF157A5A)
