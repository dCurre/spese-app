package com.dcapps.spese.core.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val italy = Locale.ITALY

/** Formatta un importo in euro, es. "12,50 €". */
fun BigDecimal.toEuro(): String =
    NumberFormat.getCurrencyInstance(italy).format(this)

/** Data breve italiana, es. "03/11/2026". */
fun Instant?.toShortDate(): String {
    if (this == null) return ""
    return DateTimeFormatter.ofPattern("dd/MM/yyyy", italy)
        .withZone(ZoneId.systemDefault())
        .format(this)
}
