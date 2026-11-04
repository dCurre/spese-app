package com.dcapps.spese.core.network.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Le date dal backend arrivano in formati eterogenei:
 *  - RFC 1123, es. "Tue, 03 Nov 2026 10:00:00 GMT"  (created_at/updated_at/joined_at)
 *  - ISO 8601, es. "2026-11-03T10:00:00" o con millis/offset
 *  - data semplice, es. "2026-11-03"  (purchased_at)
 *
 * Questo serializer prova in sequenza questi formati e, in caso di parsing impossibile,
 * restituisce `null` invece di far fallire l'intera deserializzazione.
 */
object InstantSerializer : KSerializer<Instant?> {

    private val rfc1123: DateTimeFormatter =
        DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH)

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("InstantFlexible", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant? {
        val raw: String? = when (val element = (decoder as? JsonDecoder)?.decodeJsonElement()) {
            null -> decoder.decodeString()
            JsonNull -> null
            is JsonPrimitive -> element.content.takeIf { it.isNotBlank() && it != "null" }
            else -> null
        }
        if (raw.isNullOrBlank()) return null
        return parse(raw)
    }

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Instant?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value.toString())
    }

    fun parse(raw: String): Instant? {
        // ISO Instant (con Z/offset)
        runCatching { return Instant.parse(raw) }
        // RFC 1123
        runCatching { return Instant.from(rfc1123.parse(raw)) }
        // ISO LocalDateTime (senza offset) -> assume UTC
        runCatching { return LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC) }
        // Data semplice -> inizio giornata UTC
        runCatching { return LocalDate.parse(raw).atStartOfDay(ZoneOffset.UTC).toInstant() }
        return null
    }
}
