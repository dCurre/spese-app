package com.dcapps.spese.core.network.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class BigDecimalSerializerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Amount(
        @Serializable(with = BigDecimalSerializer::class) val value: BigDecimal,
    )

    @Serializable
    private data class NullableAmount(
        @Serializable(with = BigDecimalNullableSerializer::class) val value: BigDecimal? = null,
    )

    @Test
    fun `deserializes amount given as JSON string`() {
        // Il backend manda i Decimal come stringa: "50.00".
        val r = json.decodeFromString<Amount>("""{"value":"50.00"}""")
        assertEquals(0, BigDecimal("50.00").compareTo(r.value))
    }

    @Test
    fun `deserializes amount given as JSON number`() {
        // Alcuni endpoint (purchases) mandano un numero.
        val r = json.decodeFromString<Amount>("""{"value":50.00}""")
        assertEquals(0, BigDecimal("50.00").compareTo(r.value))
    }

    @Test
    fun `serializes always as plain string`() {
        val encoded = json.encodeToString(Amount(BigDecimal("1234.50")))
        assertEquals("""{"value":"1234.50"}""", encoded)
    }

    @Test
    fun `nullable serializer tolerates missing and null and blank`() {
        assertNull(json.decodeFromString<NullableAmount>("""{}""").value)
        assertNull(json.decodeFromString<NullableAmount>("""{"value":null}""").value)
        assertNull(json.decodeFromString<NullableAmount>("""{"value":""}""").value)
    }

    @Test
    fun `nullable serializer reads a real value`() {
        val r = json.decodeFromString<NullableAmount>("""{"value":"9.99"}""")
        assertEquals(0, BigDecimal("9.99").compareTo(r.value!!))
    }
}
