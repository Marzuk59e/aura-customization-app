package com.aura.launcher.domain.repository

import com.aura.launcher.core.network.NetworkResult
import com.aura.launcher.domain.model.*
import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {
    suspend fun getWallpapers(page: Int = 1, category: String? = null): NetworkResult<List<RemoteWallpaper>>
    suspend fun getWallpaperById(id: String): NetworkResult<RemoteWallpaper>
    suspend fun applyWallpaper(imageUrl: String): Boolean
    suspend fun applyWallpaperBitmap(bitmap: android.graphics.Bitmap): Boolean
}

interface IconPackRepository {
    suspend fun getIconPacks(page: Int = 1): NetworkResult<List<RemoteIconPack>>
    suspend fun getIconPackById(id: String): NetworkResult<RemoteIconPack>
}

interface ThemeRepository {
    suspend fun getThemes(page: Int = 1): NetworkResult<List<RemoteTheme>>
    suspend fun getThemeById(id: String): NetworkResult<RemoteTheme>
}

interface WidgetRepository {
    suspend fun getWidgets(page: Int = 1): NetworkResult<List<RemoteWidget>>
}
