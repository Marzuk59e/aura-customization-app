package com.aura.launcher.data.repository

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import com.aura.launcher.core.network.NetworkResult
import com.aura.launcher.data.remote.api.*
import com.aura.launcher.domain.model.*
import com.aura.launcher.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class WallpaperRepositoryImpl(
    private val context: Context,
    private val wallpaperApi: WallpaperApi
) : WallpaperRepository {

    // Built-in seed data for offline / fallback
    private val defaultWallpapers = listOf(
        RemoteWallpaper(
            id = "wp-1",
            title = "Aura Cyber Neon",
            imageUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?q=80&w=1080",
            thumbnailUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?q=80&w=400",
            category = "Cyberpunk",
            downloads = 1420
        ),
        RemoteWallpaper(
            id = "wp-2",
            title = "Dark Horizon AMOLED",
            imageUrl = "https://images.unsplash.com/photo-1507499739999-097706ad8914?q=80&w=1080",
            thumbnailUrl = "https://images.unsplash.com/photo-1507499739999-097706ad8914?q=80&w=400",
            category = "AMOLED",
            downloads = 2100
        ),
        RemoteWallpaper(
            id = "wp-3",
            title = "Tokyo Midnight Rain",
            imageUrl = "https://images.unsplash.com/photo-1514565131-fce0801e5785?q=80&w=1080",
            thumbnailUrl = "https://images.unsplash.com/photo-1514565131-fce0801e5785?q=80&w=400",
            category = "City",
            downloads = 3400
        ),
        RemoteWallpaper(
            id = "wp-4",
            title = "Minimalist Gradient Dunes",
            imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=1080",
            thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=400",
            category = "Minimal",
            downloads = 980
        )
    )

    override suspend fun getWallpapers(page: Int, category: String?): NetworkResult<List<RemoteWallpaper>> {
        return try {
            val response = wallpaperApi.getWallpapers(page = page, categoryId = category)
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!.items.map { dto ->
                    RemoteWallpaper(
                        id = dto.id,
                        title = dto.title,
                        imageUrl = dto.imageUrl,
                        thumbnailUrl = dto.thumbnailUrl ?: dto.imageUrl,
                        category = dto.categoryId ?: "General",
                        tier = dto.tier,
                        downloads = dto.downloads
                    )
                }
                NetworkResult.Success(if (list.isNotEmpty()) list else defaultWallpapers)
            } else {
                NetworkResult.Success(defaultWallpapers)
            }
        } catch (e: Exception) {
            // Graceful fallback to offline seed data
            NetworkResult.Success(defaultWallpapers)
        }
    }

    override suspend fun getWallpaperById(id: String): NetworkResult<RemoteWallpaper> {
        val found = defaultWallpapers.find { it.id == id }
        return if (found != null) NetworkResult.Success(found) else NetworkResult.Error(404, "Wallpaper not found")
    }
    
        override suspend fun applyWallpaperBitmap(bitmap: android.graphics.Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            WallpaperManager.getInstance(context).setBitmap(bitmap)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun applyWallpaper(imageUrl: String): Boolean = withContext(Dispatchers.IO) { 
         try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val url = URL(imageUrl)
            val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
            if (bitmap != null) {
                wallpaperManager.setBitmap(bitmap)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

class IconPackRepositoryImpl(
    private val iconPackApi: IconPackApi
) : IconPackRepository {

    private val defaultIconPacks = listOf(
        RemoteIconPack(
            id = "ip-1",
            name = "Aura Glow Neon",
            description = "High-contrast neon outline icons designed for dark AMOLED setups.",
            iconCount = 1450,
            previewUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?q=80&w=400",
            tier = "free"
        ),
        RemoteIconPack(
            id = "ip-2",
            name = "Minimalist Mono",
            description = "Clean, monochromatic minimalist iconography for distraction-free focus.",
            iconCount = 2100,
            previewUrl = "https://images.unsplash.com/photo-1507499739999-097706ad8914?q=80&w=400",
            tier = "free"
        ),
        RemoteIconPack(
            id = "ip-3",
            name = "Cyberpunk 2099",
            description = "Futuristic glowing glyphs and angular elements.",
            iconCount = 980,
            previewUrl = "https://images.unsplash.com/photo-1514565131-fce0801e5785?q=80&w=400",
            tier = "premium"
        )
    )

    override suspend fun getIconPacks(page: Int): NetworkResult<List<RemoteIconPack>> {
        return try {
            val response = iconPackApi.getIconPacks(page = page)
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!.items.map { dto ->
                    RemoteIconPack(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description ?: "",
                        iconCount = dto.iconCount,
                        previewUrl = dto.previewUrl ?: dto.coverUrl ?: "",
                        tier = dto.tier,
                        author = dto.author
                    )
                }
                NetworkResult.Success(if (list.isNotEmpty()) list else defaultIconPacks)
            } else {
                NetworkResult.Success(defaultIconPacks)
            }
        } catch (e: Exception) {
            NetworkResult.Success(defaultIconPacks)
        }
    }

    override suspend fun getIconPackById(id: String): NetworkResult<RemoteIconPack> {
        val found = defaultIconPacks.find { it.id == id }
        return if (found != null) NetworkResult.Success(found) else NetworkResult.Error(404, "Icon Pack not found")
    }
}

class ThemeRepositoryImpl(
    private val themeApi: ThemeApi
) : ThemeRepository {

    private val defaultThemes = listOf(
        RemoteTheme(
            id = "theme-1",
            name = "Cyberpunk Aurora",
            description = "Vibrant violet & neon cyan futuristic experience.",
            primaryColor = "#7000FF",
            secondaryColor = "#00F2FE",
            previewUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?q=80&w=400"
        ),
        RemoteTheme(
            id = "theme-2",
            name = "AMOLED Obsidian",
            description = "Pitch black luxury styling with subtle titanium accents.",
            primaryColor = "#222232",
            secondaryColor = "#9E9EAA",
            previewUrl = "https://images.unsplash.com/photo-1507499739999-097706ad8914?q=80&w=400"
        ),
        RemoteTheme(
            id = "theme-3",
            name = "Sunset Synthwave",
            description = "Warm retro pink and amber sunset vibes.",
            primaryColor = "#FF007A",
            secondaryColor = "#FFB800",
            previewUrl = "https://images.unsplash.com/photo-1514565131-fce0801e5785?q=80&w=400"
        )
    )

    override suspend fun getThemes(page: Int): NetworkResult<List<RemoteTheme>> {
        return try {
            val response = themeApi.getThemes(page = page)
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!.items.map { dto ->
                    RemoteTheme(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description ?: "",
                        primaryColor = dto.primaryColor,
                        secondaryColor = dto.secondaryColor,
                        previewUrl = dto.previewUrl ?: "",
                        tier = dto.tier
                    )
                }
                NetworkResult.Success(if (list.isNotEmpty()) list else defaultThemes)
            } else {
                NetworkResult.Success(defaultThemes)
            }
        } catch (e: Exception) {
            NetworkResult.Success(defaultThemes)
        }
    }

    override suspend fun getThemeById(id: String): NetworkResult<RemoteTheme> {
        val found = defaultThemes.find { it.id == id }
        return if (found != null) NetworkResult.Success(found) else NetworkResult.Error(404, "Theme not found")
    }
}
