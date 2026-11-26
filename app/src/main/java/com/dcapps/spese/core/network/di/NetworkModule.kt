package com.dcapps.spese.core.network.di

import com.dcapps.spese.BuildConfig
import com.dcapps.spese.core.network.AuthInterceptor
import com.dcapps.spese.core.network.SpeseApi
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true      // il backend può aggiungere campi non modellati
        explicitNulls = false         // non serializzare i null dei campi opzionali (PATCH parziali)
        coerceInputValues = true      // null su campi non-null -> default
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(auth: FirebaseAuth): OkHttpClient {
        val builder = OkHttpClient.Builder()
            // Render può avere cold start lenti: timeout generosi.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(BuildConfig.API_BASE_URL.toHttpUrl()) { auth.blockingIdToken() })

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                    redactHeader("Authorization")
                }
            )
        }
        return builder.build()
    }

    /**
     * Token dell'utente corrente, rinnovato da Firebase se scaduto. Bloccante: va bene perché gli
     * interceptor girano sui thread di OkHttp. Se non ottenibile (es. offline) è un errore di rete.
     */
    private fun FirebaseAuth.blockingIdToken(): String? {
        val user = currentUser ?: return null
        return try {
            Tasks.await(user.getIdToken(false), 30, TimeUnit.SECONDS).token
        } catch (e: Exception) {
            throw IOException("Token Firebase non disponibile", e)
        }
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideSpeseApi(retrofit: Retrofit): SpeseApi = retrofit.create(SpeseApi::class.java)
}
