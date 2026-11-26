package com.dcapps.spese.core.network

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Aggiunge l'ID token Firebase alle sole chiamate verso il backend: il client OkHttp è condiviso
 * con GitHub (update e bug report), che non deve mai riceverlo. [token] è null senza utente loggato.
 */
class AuthInterceptor(
    private val apiUrl: HttpUrl,
    private val token: () -> String?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val isApi = url.scheme == apiUrl.scheme && url.host == apiUrl.host && url.port == apiUrl.port
        val idToken = if (isApi) token() else null
        return chain.proceed(
            if (idToken == null) request
            else request.newBuilder().header("Authorization", "Bearer $idToken").build()
        )
    }
}
