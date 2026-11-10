package com.dcapps.spese.core.model

import com.dcapps.spese.core.network.serialization.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Statistiche aggregate delle spese PERSONALI dell'utente (solo quelle pagate da lui), calcolate
 * lato backend dall'endpoint `GET /api/expenses/stats`. Alimenta la dashboard statistiche.
 *
 * Gli importi arrivano come stringa JSON (contratto Decimal del backend) → [BigDecimalSerializer].
 */
@Serializable
data class ExpenseStats(
    val currency: String = "EUR",
    val from: String? = null,
    val to: String? = null,
    val granularity: String = "month",
    @Serializable(BigDecimalSerializer::class) val total: BigDecimal = BigDecimal.ZERO,
    val count: Int = 0,
    @SerialName("previous_total")
    @Serializable(BigDecimalSerializer::class) val previousTotal: BigDecimal = BigDecimal.ZERO,
    @SerialName("by_period") val byPeriod: List<PeriodBucket> = emptyList(),
    @SerialName("by_type") val byType: List<TypeBucket> = emptyList(),
)

/** Totale di un intervallo temporale (es. "2026-09" per il mese). */
@Serializable
data class PeriodBucket(
    val period: String,
    @Serializable(BigDecimalSerializer::class) val total: BigDecimal = BigDecimal.ZERO,
    val count: Int = 0,
)

/** Totale per tipologia di spesa. [expenseTypeId]/[name] null = spese senza categoria. */
@Serializable
data class TypeBucket(
    @SerialName("expense_type_id") val expenseTypeId: Long? = null,
    val name: String? = null,
    @Serializable(BigDecimalSerializer::class) val total: BigDecimal = BigDecimal.ZERO,
    val count: Int = 0,
)
