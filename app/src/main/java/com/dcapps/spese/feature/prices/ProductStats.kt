package com.dcapps.spese.feature.prices

import com.dcapps.spese.core.model.Purchase

/**
 * Statistiche di prezzo di un prodotto, calcolate dagli acquisti. Funzione PURA (nessuna dipendenza
 * da Android/UI) così è testabile a unità: la UI (ProductStatsCard) la usa invece di calcolare inline.
 *
 * @property minUnitPrice prezzo unitario minimo di sempre.
 * @property minStore negozio dell'acquisto al prezzo minimo (se presente).
 * @property lastUnitPrice prezzo unitario dell'acquisto più recente (per data).
 * @property variationPct variazione % dell'ultimo prezzo rispetto al minimo (0 se è al minimo o sotto).
 */
data class ProductStats(
    val minUnitPrice: Double,
    val minStore: String?,
    val lastUnitPrice: Double,
    val variationPct: Double,
) {
    /** True se l'ultimo prezzo coincide col minimo (o è inferiore): "sei al minimo". */
    val isAtMinimum: Boolean get() = variationPct <= 0.0
}

/**
 * Calcola le statistiche dagli acquisti. Ritorna null se non ci sono acquisti (niente da mostrare).
 * "Ultimo" = acquisto con `purchasedAt` massimo (le date sono stringhe `yyyy-MM-dd`, ordinabili come
 * stringa); a parità di data, l'ultimo nell'ordine della lista. "Minimo" = minor `unitPrice`.
 */
fun productStats(purchases: List<Purchase>): ProductStats? {
    if (purchases.isEmpty()) return null
    val minPurchase = purchases.minByOrNull { it.unitPrice }
    val lastPurchase = purchases.maxByOrNull { it.purchasedAt }
    val minPrice = minPurchase?.unitPrice ?: 0.0
    val lastPrice = lastPurchase?.unitPrice ?: 0.0
    val variationPct = if (minPrice > 0.0) ((lastPrice - minPrice) / minPrice * 100.0) else 0.0
    return ProductStats(
        minUnitPrice = minPrice,
        minStore = minPurchase?.store?.takeIf { it.isNotBlank() },
        lastUnitPrice = lastPrice,
        variationPct = variationPct,
    )
}
