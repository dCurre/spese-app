package com.dcapps.spese.core.update.di

import com.dcapps.spese.core.bugreport.BugReportApi
import com.dcapps.spese.core.update.GithubApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UpdateModule {

    private fun githubRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideGithubApi(client: OkHttpClient, json: Json): GithubApi =
        githubRetrofit(client, json).create(GithubApi::class.java)

    @Provides
    @Singleton
    fun provideBugReportApi(client: OkHttpClient, json: Json): BugReportApi =
        githubRetrofit(client, json).create(BugReportApi::class.java)
}
