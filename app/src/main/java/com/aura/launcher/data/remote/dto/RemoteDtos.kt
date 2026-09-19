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

data class CategoryDto(
    val id: String,
    val name: String,
    val scope: String = "wallpaper",
    val itemCount: Int = 0
)

// Subset of a published widget document (public/widgets). Field names match
// what the admin panel stores: `kind` (not type), `widgetVersion`, `tier`.
data class PublicWidgetDto(
    val id: String,
    val name: String,
    val kind: String = "clock",
    val price: Double = 0.0,
    val tier: String = "free",
    val previewUrl: String? = null,
    val widgetVersion: String = "1.0.0"
) {
    val isFree: Boolean get() = tier != "premium"
}

// public/featured returns whole (sanitised) content items; only the id is needed.
data class PublicFeaturedDto(
    val id: String
)

// Mirrors the AuthUser shape returned by GET /auth/me on the new Node
// backend (routes/auth.routes.ts) — verified against a Supabase access
// token. Backend owns this row (Postgres
// `users` table) — the app never writes user profile fields directly.
data class UserProfileDto(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String? = null,
    val role: String? = null,
    val status: String? = null,
    val permissions: List<String>? = null,
    val lastLoginAt: String? = null
)
