package com.aura.launcher.core.network

import com.aura.launcher.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Separate Retrofit instance for Supabase's GoTrue REST API
 * (BuildConfig.SUPABASE_URL). Deliberately NOT the same client as ApiClient:
 * this one authenticates with the Supabase anon key, never with our own
 * backend's token, and our own backend's AuthInterceptor never touches these
 * calls.
 */
object SupabaseClient {

    fun createRetrofit(): Retrofit {
        val anonKey = BuildConfig.SUPABASE_ANON_KEY

        val apiKeyInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
            // Every call here defaults to the anon key as Authorization,
            // EXCEPT the recovery update-user call (Phase 4.4), which sets
            // its own Authorization: Bearer <recovery access_token> via
            // @Header — don't stomp that with the anon key.
            if (original.header("Authorization") == null) {
                builder.header("Authorization", "Bearer $anonKey")
            }
            chain.proceed(builder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            // Login/signup bodies carry the password and tokens, so never
            // log them in release builds.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            // Even in debug, keep credentials out of Logcat.
            redactHeader("apikey")
            redactHeader("Authorization")
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        val baseUrl = BuildConfig.SUPABASE_URL.let {
            if (it.endsWith("/")) it else "$it/"
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
