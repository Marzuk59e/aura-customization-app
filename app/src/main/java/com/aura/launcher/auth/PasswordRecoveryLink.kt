package com.aura.launcher.auth

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tokens Supabase GoTrue attaches to the password-recovery redirect link,
 * e.g. auralauncher://reset-callback#access_token=...&refresh_token=...&type=recovery
 *
 * GoTrue puts these in the URL *fragment* (after '#'), not the query string
 * — Uri.getQueryParameter() won't see them, so [fromDeepLink] parses the
 * fragment by hand.
 */
data class PasswordRecoveryLink(
    val accessToken: String,
    val refreshToken: String?,
    val type: String?
) {
    companion object {
        private const val SCHEME = "auralauncher"
        private const val HOST = "reset-callback"

        /** Returns null for any URI that isn't our recovery callback link. */
        fun fromDeepLink(uri: Uri?): PasswordRecoveryLink? {
            if (uri == null || uri.scheme != SCHEME || uri.host != HOST) return null

            val fragment = uri.fragment ?: uri.encodedQuery ?: return null
            val params = fragment.split("&")
                .mapNotNull { pair ->
                    val idx = pair.indexOf('=')
                    if (idx == -1) null else pair.substring(0, idx) to Uri.decode(pair.substring(idx + 1))
                }
                .toMap()

            val accessToken = params["access_token"] ?: return null
            return PasswordRecoveryLink(
                accessToken = accessToken,
                refreshToken = params["refresh_token"],
                type = params["type"]
            )
        }
    }
}

/**
 * Process-wide holder for the most recent recovery link. It arrives via an
 * Intent (MainActivity), but nothing reads [current] yet — Phase 4.4's
 * reset-password screen/ViewModel will collect it to know it's in recovery
 * mode and to get the access token needed to call GoTrue's update-user
 * endpoint.
 */
object PasswordRecoveryLinkHolder {
    private val _current = MutableStateFlow<PasswordRecoveryLink?>(null)
    val current: StateFlow<PasswordRecoveryLink?> = _current

    fun publish(link: PasswordRecoveryLink) {
        _current.value = link
    }

    /** Call once the recovery flow has consumed the link, so re-collecting doesn't replay it. */
    fun consume() {
        _current.value = null
    }
}
