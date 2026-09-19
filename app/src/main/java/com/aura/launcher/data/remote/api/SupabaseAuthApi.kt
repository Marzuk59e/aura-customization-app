package com.aura.launcher.data.remote.api

import com.aura.launcher.data.remote.dto.SupabaseAuthResponseDto
import com.aura.launcher.data.remote.dto.SupabaseOtpRequestDto
import com.aura.launcher.data.remote.dto.SupabasePasswordGrantRequestDto
import com.aura.launcher.data.remote.dto.SupabaseRefreshRequestDto
import com.aura.launcher.data.remote.dto.SupabaseSignUpRequestDto
import com.aura.launcher.data.remote.dto.SupabaseUpdateUserRequestDto
import com.aura.launcher.data.remote.dto.SupabaseUserDto
import com.aura.launcher.data.remote.dto.SupabaseVerifyOtpRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * Direct GoTrue REST calls (Supabase's auth server) — hit via the dedicated
 * Retrofit instance from SupabaseClient (apikey header, Supabase base URL),
 * NOT the app's own backend Retrofit (ApiClient/AuthInterceptor). Kept
 * completely separate from AuthApi on purpose: this repository never sees a
 * Firebase or backend token, only Supabase's.
 */
interface SupabaseAuthApi {

    @POST("auth/v1/signup")
    suspend fun signUp(@Body body: SupabaseSignUpRequestDto): Response<SupabaseAuthResponseDto>

    @POST("auth/v1/token")
    suspend fun signInWithPassword(
        @Query("grant_type") grantType: String = "password",
        @Body body: SupabasePasswordGrantRequestDto
    ): Response<SupabaseAuthResponseDto>

    @POST("auth/v1/token")
    suspend fun refreshToken(
        @Query("grant_type") grantType: String = "refresh_token",
        @Body body: SupabaseRefreshRequestDto
    ): Response<SupabaseAuthResponseDto>

    // Emails a 6-digit one-time code (no link) — see SupabaseOtpRequestDto
    // for the email-template caveat. No response body on success (204/200).
    // GoTrue returns 200 here whether or not the address has an account, by
    // design, so this never leaks which emails are registered.
    @POST("auth/v1/otp")
    suspend fun sendOtp(@Body body: SupabaseOtpRequestDto): Response<Unit>

    // Checks the typed 6-digit code and, on success, returns a real session
    // (access_token/refresh_token/user) — GoTrue proves "this is the person
    // who owns the email" the same way it would after a password login.
    @POST("auth/v1/verify")
    suspend fun verifyOtp(@Body body: SupabaseVerifyOtpRequestDto): Response<SupabaseAuthResponseDto>

    // The verified-code access_token stands in for a normal session here —
    // that's what proves the user typed the right code. It is NOT the app's
    // usual logged-in session token (that one comes from login()/signUp()).
    @PUT("auth/v1/user")
    suspend fun updateUser(
        @Header("Authorization") authorization: String,
        @Body body: SupabaseUpdateUserRequestDto
    ): Response<SupabaseUserDto>
}
