package com.aura.launcher.data.remote.api

import com.aura.launcher.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Content endpoints hit the backend's PUBLIC routes (/api/v1/public/...), not
 * the admin CRUD routes (/wallpapers, /icon-packs, /themes), which require an
 * admin permission and reject normal app users.
 *
 * The public routes return a PLAIN JSON ARRAY of items (published only).
 * There is no pagination wrapper and no server-side filtering, so page /
 * category / tier / status query params are not sent. Premium items come back
 * with their download URLs stripped and `_locked: true`.
 */
interface WallpaperApi {
    @GET("public/wallpapers")
    suspend fun getWallpapers(): Response<List<WallpaperDto>>

    @GET("public/wallpapers/{id}")
    suspend fun getWallpaperById(@Path("id") id: String): Response<WallpaperDto>
}

interface IconPackApi {
    @GET("public/icon-packs")
    suspend fun getIconPacks(): Response<List<IconPackDto>>

    @GET("public/icon-packs/{id}")
    suspend fun getIconPackById(@Path("id") id: String): Response<IconPackDto>
}

interface ThemeApi {
    @GET("public/themes")
    suspend fun getThemes(): Response<List<ThemeDto>>

    @GET("public/themes/{id}")
    suspend fun getThemeById(@Path("id") id: String): Response<ThemeDto>
}

/**
 * NOTE: the backend has no public equivalents for these two: /app/versions and
 * /app/config exist but are admin-only (require app.versions.read /
 * app.config.read). Nothing in the app calls them yet; add public endpoints
 * on the backend before wiring them up.
 */
interface RemoteConfigApi {
    @GET("app/version")
    suspend fun getAppVersion(): Response<AppVersionDto>

    @GET("app/config")
    suspend fun getRemoteConfig(): Response<RemoteConfigDto>
}

interface PublicApi {
    @GET("public/widgets")
    suspend fun getPublicWidgets(): Response<List<PublicWidgetDto>>

    // Backend returns full (sanitised) item objects here, not bare ids.
    @GET("public/featured")
    suspend fun getPublicFeatured(): Response<List<PublicFeaturedDto>>

    @GET("public/categories")
    suspend fun getPublicCategories(): Response<List<CategoryDto>>
}

interface AuthApi {
    // Backend verifies the Supabase access token (sent via Authorization
    // header by AuthInterceptor) and returns/provisions the canonical user
    // profile.
    @GET("auth/me")
    suspend fun me(): Response<UserProfileDto>
}
