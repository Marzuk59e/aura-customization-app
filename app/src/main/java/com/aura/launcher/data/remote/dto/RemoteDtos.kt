package com.aura.launcher.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WallpaperDto(
    val id: String,
    val title: String,
    val imageUrl: String,
    val thumbnailUrl: String? = null,
    val categoryId: String? = null,
    val orientation: String = "portrait",
    val tier: String = "free",
    val status: String = "published",
    val palette: List<String>? = null,
    val tags: List<String>? = null,
    val downloads: Int = 0
)

data class IconPackDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val iconCount: Int = 0,
    val previewUrl: String? = null,
    val coverUrl: String? = null,
    val tier: String = "free",
    val status: String = "published",
    val author: String = "Aura Studio"
)

data class IconMappingDto(
    val id: String? = null,
    val name: String,
    val componentName: String,
    val imageUrl: String,
    val format: String = "webp"
)

data class ThemeDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val iconPackId: String? = null,
    val wallpaperId: String? = null,
    val primaryColor: String = "#7000FF",
    val secondaryColor: String = "#00F2FE",
    val textColor: String = "#FFFFFF",
    val backgroundColor: String = "#0A0A0E",
    val previewUrl: String? = null,
    val tier: String = "free",
    val status: String = "published"
)

data class WidgetDto(
    val id: String,
    val name: String,
    val kind: String = "clock",
    val defaultSize: String = "4x2",
    val previewUrl: String? = null,
    val tier: String = "free"
)

data class AppVersionDto(
    val version: String,
    val versionCode: Int,
    val channel: String = "production",
    val minSupportedVersion: String = "1.0.0",
    val forceUpdate: Boolean = false,
    val releaseNotes: String? = null,
    val downloadUrl: String? = null
)

data class RemoteConfigDto(
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String? = null,
    val featuredThemeId: String? = null,
    val featuredWallpaperId: String? = null,
    val apiRateLimit: Int = 60,
    val contentCacheTtl: Int = 3600
)
