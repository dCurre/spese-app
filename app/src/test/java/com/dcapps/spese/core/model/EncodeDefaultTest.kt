package com.dcapps.spese.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica che list_type sia SEMPRE serializzato (via @EncodeDefault) anche quando vale il default
 * "personal", con lo stesso Json dell'app (explicitNulls=false, encodeDefaults NON impostato).
 * Se questo test passa ma il backend crea comunque shared, il bug è lato backend.
 */
class EncodeDefaultTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun `SyncExpensesListCreate serializza list_type anche col default personal`() {
        val payload = SyncExpensesListCreate(
            tempId = "L1", clientId = "cid", userId = 1L, name = "Test",
            // listType non specificato → usa il default "personal"
        )
        val out = json.encodeToString(payload)
        assertTrue("list_type deve essere presente nel JSON: $out", out.contains("\"list_type\""))
        assertTrue("list_type deve valere personal: $out", out.contains("\"list_type\":\"personal\""))
    }

    @Test
    fun `SyncListCreate checklist serializza list_type col default personal`() {
        val payload = SyncListCreate(
            tempId = "L1", clientId = "cid", ownerId = 1L, name = "Test",
        )
        val out = json.encodeToString(payload)
        assertTrue("list_type deve essere presente: $out", out.contains("\"list_type\":\"personal\""))
    }
}
