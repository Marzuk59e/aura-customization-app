package com.aura.launcher.core.network

import com.aura.launcher.BuildConfig
import com.aura.launcher.core.datastore.AuthPreferences
import com.aura.launcher.data.remote.auth.SupabaseSessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    fun createRetrofit(
        authPreferences: AuthPreferences,
        supabaseSessionManager: SupabaseSessionManager,
        baseUrl: String = BuildConfig.API_BASE_URL
    ): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            // Access tokens flow through here (Authorization header, refresh
            // responses), so never log bodies/headers in release builds.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            // Even in debug, keep the token out of Logcat.
            redactHeader("Authorization")
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authPreferences, supabaseSessionManager))
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
