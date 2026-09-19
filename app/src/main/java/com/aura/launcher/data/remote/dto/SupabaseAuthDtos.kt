package com.aura.launcher.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request/response shapes for Supabase's GoTrue REST API (auth/v1/...), called
 * directly over plain Retrofit — no Supabase Kotlin SDK, per the Phase 1
 * decision (see SupabaseAuthApi).
 */

data class SupabaseSignUpRequestDto(
    val email: String,
    val password: String,
    /** Becomes the user's user_metadata (e.g. full_name) — never a trust decision by itself. */
    val data: Map<String, String>? = null
)

data class SupabasePasswordGrantRequestDto(
    val email: String,
    val password: String
)

data class SupabaseRefreshRequestDto(
    @SerializedName("refresh_token") val refreshToken: String
)

/**
 * Body for /auth/v1/otp — asks GoTrue to email a 6-digit one-time code to
 * [email] (createUser=false so this never silently creates a new account
 * during "forgot password"). No response body on success.
 *
 * NOTE: whether the email shows a clickable link or a plain 6-digit code is
 * controlled by the "Magic Link" email template in the Supabase project's
 * Auth settings, not by this request — the template must use the
 * `{{ .Token }}` variable (not `{{ .ConfirmationURL }}`) for the code-only
 * flow this app implements.
 */
data class SupabaseOtpRequestDto(
    val email: String,
    @SerializedName("create_user") val createUser: Boolean = false
)

/**
 * Body for /auth/v1/verify — checks the 6-digit [token] the user typed in
 * against the one GoTrue emailed. On success this returns a full session
 * (access_token/refresh_token/user), exactly like a normal login — that
 * session is what proves the code was correct and is used both to set a
 * new password and, if the user skips resetting, to sign them straight in.
 */
data class SupabaseVerifyOtpRequestDto(
    val type: String = "recovery",
    val email: String,
    val token: String
)

/**
 * Body for PUT /auth/v1/user — sets the new password using the recovery
 * access_token as Authorization (see SupabaseAuthApi.updateUser). Called
 * once, right after the user taps the emailed reset link.
 */
data class SupabaseUpdateUserRequestDto(
    val password: String
)

data class SupabaseUserDto(
    val id: String,
    val email: String? = null,
    @SerializedName("user_metadata") val userMetadata: Map<String, Any>? = null
)

/**
 * Common response shape for /auth/v1/signup, /auth/v1/token?grant_type=password
 * and /auth/v1/token?grant_type=refresh_token. Token fields are null when
 * Supabase requires email confirmation before a session is issued (signup
 * only) — callers must check for that instead of assuming a session exists.
 */
data class SupabaseAuthResponseDto(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("expires_in") val expiresIn: Long? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    val user: SupabaseUserDto? = null
)

/** GoTrue error bodies vary a bit by version; we read whichever of these is present. */
data class SupabaseErrorResponseDto(
    val msg: String? = null,
    val error: String? = null,
    @SerializedName("error_description") val errorDescription: String? = null,
    @SerializedName("error_code") val errorCode: String? = null
)
