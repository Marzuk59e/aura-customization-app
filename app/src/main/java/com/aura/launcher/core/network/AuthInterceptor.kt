package com.aura.launcher.core.network

import com.aura.launcher.core.datastore.AuthPreferences
import com.aura.launcher.data.remote.auth.SupabaseSessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Attaches the current Supabase access token to every backend request
 * (guest sessions have no Supabase session, so they simply send no
 * Authorization header).
 *
 * [authPreferences] isn't read directly here anymore (token lookup goes
 * through [supabaseSessionManager]) but stays a constructor param since
 * ApiClient already wires it in and future headers may need it again.
 */
class AuthInterceptor(
    private val authPreferences: AuthPreferences,
    private val supabaseSessionManager: SupabaseSessionManager,
    private val tokenProvider: (() -> String?)? = null
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = tokenProvider?.invoke() ?: runBlocking {
            try {
                supabaseSessionManager.getValidAccessToken()
            } catch (e: Exception) {
                null
            }
        }

        val response = chain.proceed(buildRequest(originalRequest, token))

        // The token looked valid when checked above but the backend still
        // rejected it (clock skew, just-revoked, etc.) — force one refresh
        // and retry before giving up. Skipped when tokenProvider is set
        // (e.g. tests) since there's no session to refresh in that case.
        if (response.code == 401 && tokenProvider == null && !token.isNullOrBlank()) {
            val refreshed = runBlocking {
                try {
                    supabaseSessionManager.forceRefresh()
                } catch (e: Exception) {
                    null
                }
            }
            if (!refreshed.isNullOrBlank() && refreshed != token) {
                response.close()
                return chain.proceed(buildRequest(originalRequest, refreshed))
            }
        }

        return response
    }

    private fun buildRequest(original: Request, token: String?): Request {
        val builder = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("X-Client-Platform", "Android-Launcher")

        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }

        return builder.build()
    }
}
