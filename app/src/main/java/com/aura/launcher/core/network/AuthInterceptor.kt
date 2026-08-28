package com.aura.launcher.core.network

import com.aura.launcher.core.datastore.AuthPreferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val authPreferences: AuthPreferences,
    private val tokenProvider: (() -> String?)? = null
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        requestBuilder.addHeader("Content-Type", "application/json")
        requestBuilder.addHeader("Accept", "application/json")
        requestBuilder.addHeader("X-Client-Platform", "Android-Launcher")

        val token = tokenProvider?.invoke() ?: runBlocking {
            authPreferences.activeUserIdFlow.firstOrNull()
        }

        if (!token.isNullOrBlank() && !token.startsWith("guest_")) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
