package com.aura.launcher.data.remote.api

import com.aura.launcher.core.network.PaginatedResponse
import com.aura.launcher.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface WallpaperApi {
    @GET("wallpapers")
    suspend fun getWallpapers(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("categoryId") categoryId: String? = null,
        @Query("tier") tier: String? = null,
        @Query("status") status: String = "published"
    ): Response<PaginatedResponse<WallpaperDto>>

    @GET("wallpapers/{id}")
    suspend fun getWallpaperById(@Path("id") id: String): Response<WallpaperDto>
}

interface IconPackApi {
    @GET("icon-packs")
    suspend fun getIconPacks(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("tier") tier: String? = null,
        @Query("status") status: String = "published"
    ): Response<PaginatedResponse<IconPackDto>>

    @GET("icon-packs/{id}")
    suspend fun getIconPackById(@Path("id") id: String): Response<IconPackDto>

    @GET("icon-packs/{id}/icons")
    suspend fun getIconPackMappings(@Path("id") id: String): Response<List<IconMappingDto>>
}

interface ThemeApi {
    @GET("themes")
    suspend fun getThemes(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("status") status: String = "published"
    ): Response<PaginatedResponse<ThemeDto>>

    @GET("themes/{id}")
    suspend fun getThemeById(@Path("id") id: String): Response<ThemeDto>
}

interface WidgetApi {
    @GET("widgets")
    suspend fun getWidgets(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<PaginatedResponse<WidgetDto>>
}

interface RemoteConfigApi {
    @GET("app/version")
    suspend fun getAppVersion(): Response<AppVersionDto>

    @GET("app/config")
    suspend fun getRemoteConfig(): Response<RemoteConfigDto>
}

interface PublicApi {
    @GET("public/home")
    suspend fun getPublicHome(): Response<PublicHomeDto>

    @GET("public/widgets")
    suspend fun getPublicWidgets(): Response<List<PublicWidgetDto>>

    @GET("public/featured")
    suspend fun getPublicFeatured(): Response<List<String>>

    @GET("public/categories")
    suspend fun getPublicCategories(): Response<List<CategoryDto>>
}

interface AuthApi {
    // Backend verifies the Firebase ID token (sent via Authorization header by
    // AuthInterceptor) and returns/provisions the canonical user profile —
    // same endpoint the admin panel calls after Firebase sign-in.
    @GET("auth/me")
    suspend fun me(): Response<UserProfileDto>
}

/**
 * Secure device authentication endpoints — replaces the old simulated OTP
 * password-reset step. See DeviceAuthRepositoryImpl for the full flow and
 * RemoteDtos.kt for exactly what each call does and does not send.
 *
 * NOTE for backend implementation (aura-launcher.unaux.com PHP backend):
 *  - register: requires the normal Authorization: Bearer <FirebaseIdToken>
 *    header (already attached by AuthInterceptor). Store {deviceId,
 *    publicKey, deviceLabel, algorithm, uid} — never a private key.
 *  - challenge: unauthenticated (the user is by definition logged out at
 *    this point). Look up whether {email, deviceId} has an active
 *    registration; if so, generate + store a short-lived random challenge
 *    server-side and return it.
 *  - verify-reset: unauthenticated. Verify `signature` against the stored
 *    public key for {email, deviceId} over the exact `challenge` string
 *    issued by /challenge (and that it hasn't expired/been used already),
 *    then call the Firebase Admin SDK to set the new password. Only on a
 *    verified signature should the password actually change.
 */
interface DeviceAuthApi {
    @POST("auth/devices/register")
    suspend fun registerDevice(@Body body: DeviceRegisterRequestDto): Response<DeviceRegisterResponseDto>

    @POST("auth/devices/challenge")
    suspend fun requestChallenge(@Body body: DeviceChallengeRequestDto): Response<DeviceChallengeResponseDto>

    @POST("auth/devices/verify-reset")
    suspend fun verifyAndResetPassword(@Body body: DeviceVerifyResetRequestDto): Response<DeviceVerifyResetResponseDto>

    @POST("auth/devices/verify-login")
    suspend fun verifyAndLogin(@Body body: DeviceVerifyLoginRequestDto): Response<DeviceVerifyLoginResponseDto>
}

