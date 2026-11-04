package com.dcapps.spese.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette categorica per i grafici: ordine fisso, assegnata per indice (mai ciclata in modo
 * casuale). Tinte scelte per essere distinguibili anche con deficit di visione dei colori;
 * l'identità nei grafici è comunque rafforzata da etichette dirette (nome + valore), mai
 * affidata al solo colore. Se le categorie superano la palette, le eccedenti confluiscono
 * in "Altro" (colore [chartOther]).
 */
val ChartCategorical: List<Color> = listOf(
    Color(0xFF4E79A7), // blu
    Color(0xFFF28E2B), // arancio
    Color(0xFF59A14F), // verde
    Color(0xFFE15759), // rosso
    Color(0xFFB07AA1), // viola
    Color(0xFF76B7B2), // teal
    Color(0xFFEDC948), // giallo
    Color(0xFFFF9DA7), // rosa
)

val chartOther: Color = Color(0xFF9C9C9C)

/** Colore categorico per [index], con fallback su [chartOther] oltre la palette. */
fun categoricalColor(index: Int): Color =
    ChartCategorical.getOrElse(index) { chartOther }
