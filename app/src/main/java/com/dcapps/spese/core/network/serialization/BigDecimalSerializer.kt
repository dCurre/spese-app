package com.dcapps.spese.core.network.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal

/**
 * Il backend serializza gli importi (`amount`) come **stringa JSON** (es. "50.00") quando
 * sono `Decimal`, ma come numero in alcuni endpoint (es. purchases). Questo serializer
 * accetta entrambe le forme in input e scrive sempre una stringa in output.
 */
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement()
        return if (element is JsonPrimitive) {
            BigDecimal(element.content)
        } else {
            BigDecimal(decoder.decodeString())
        }
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }
}

/** Variante nullable: tollera null/assenza senza esplodere. */
object BigDecimalNullableSerializer : KSerializer<BigDecimal?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimalNullable", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal? {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return null
        if (element is JsonPrimitive) {
            if (element.content.isBlank() || element.content == "null") return null
            return BigDecimal(element.content)
        }
        return null
    }

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: BigDecimal?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value.toPlainString())
    }
}
