package com.aura.launcher.core.network

import com.aura.launcher.core.datastore.AuthPreferences
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
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

        // Backend verifies a real Firebase ID token (same as the admin panel),
        // not the local Room user id — guest sessions have no Firebase user,
        // so they simply send no Authorization header.
        val token = tokenProvider?.invoke() ?: runBlocking {
            try {
                FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            } catch (e: Exception) {
                null
            }
        }

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
