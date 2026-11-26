package com.dcapps.spese.core.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInterceptorTest {

    /** Header Authorization che uscirebbe verso [url]; l'ultimo interceptor risponde senza rete. */
    private fun authHeader(url: String, token: String?): String? {
        var seen: String? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor("https://api.example.com/".toHttpUrl()) { token })
            .addInterceptor { chain ->
                seen = chain.request().header("Authorization")
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body("".toResponseBody()).build()
            }
            .build()
        client.newCall(Request.Builder().url(url).build()).execute().close()
        return seen
    }

    @Test
    fun `il token va al backend`() =
        assertEquals("Bearer abc", authHeader("https://api.example.com/api/users", "abc"))

    @Test
    fun `il token non va ad altri host ne in chiaro`() {
        assertNull(authHeader("https://api.github.com/repos", "abc"))
        assertNull(authHeader("http://api.example.com/api/users", "abc"))
    }

    @Test
    fun `senza utente nessun header`() =
        assertNull(authHeader("https://api.example.com/api/users", null))
}
