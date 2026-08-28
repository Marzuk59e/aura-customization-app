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
