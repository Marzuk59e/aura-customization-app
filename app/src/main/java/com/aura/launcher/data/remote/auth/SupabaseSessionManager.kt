package com.aura.launcher.data.remote.auth

import com.aura.launcher.core.datastore.AuthPreferences
import com.aura.launcher.data.remote.api.SupabaseAuthApi
import com.aura.launcher.data.remote.dto.SupabaseRefreshRequestDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the "is my Supabase access token still good, and if not, get me a new
 * one" logic. Token *storage* lives in AuthPreferences (DataStore); this
 * class adds the network refresh capability on top of it.
 *
 * Built in Phase 2 as standalone capability; AuthInterceptor doesn't call
 * this yet (Phase 3 wires that up), but AuthRepositoryImpl already uses it
 * after every sign-in so a valid session survives past the access token's
 * ~1 hour lifetime.
 */
class SupabaseSessionManager(
    private val authPreferences: AuthPreferences,
    private val supabaseAuthApi: SupabaseAuthApi
) {
    private val refreshMutex = Mutex()

    companion object {
        // Refresh a little before actual expiry so a request never races a
        // token that dies mid-flight.
        private const val EXPIRY_BUFFER_MS = 60_000L
    }

    /**
     * Returns a currently-valid access token, refreshing it first if it's
     * expired or about to expire. Returns null if there's no session at all,
     * or if refresh itself fails (e.g. the refresh token was revoked) — the
     * caller should treat null as "no longer logged in".
     */
    suspend fun getValidAccessToken(): String? {
        val storedToken = authPreferences.getSupabaseAccessToken() ?: return null

        if (!isExpiringSoon(authPreferences.getSupabaseTokenExpiresAt())) {
            return storedToken
        }

        return refreshMutex.withLock {
            // Another caller may have already refreshed while we were
            // waiting for the lock — re-check before hitting the network again.
            val currentToken = authPreferences.getSupabaseAccessToken()
            if (currentToken != null && !isExpiringSoon(authPreferences.getSupabaseTokenExpiresAt())) {
                return@withLock currentToken
            }

            val refreshToken = authPreferences.getSupabaseRefreshToken() ?: return@withLock null
            refreshWith(refreshToken)
        }
    }

    /** Forces a refresh regardless of the stored expiry. Used after a 401 in Phase 3. */
    suspend fun forceRefresh(): String? {
        return refreshMutex.withLock {
            val refreshToken = authPreferences.getSupabaseRefreshToken() ?: return@withLock null
            refreshWith(refreshToken)
        }
    }

    private suspend fun refreshWith(refreshToken: String): String? {
        return try {
            val response = supabaseAuthApi.refreshToken(body = SupabaseRefreshRequestDto(refreshToken))
            val body = response.body()
            if (response.isSuccessful && body?.accessToken != null && body.refreshToken != null) {
                authPreferences.saveSupabaseTokens(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    expiresInSeconds = body.expiresIn ?: 3600L
                )
                body.accessToken
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isExpiringSoon(expiresAtMillis: Long?): Boolean {
        if (expiresAtMillis == null) return true
        return System.currentTimeMillis() >= (expiresAtMillis - EXPIRY_BUFFER_MS)
    }
}
