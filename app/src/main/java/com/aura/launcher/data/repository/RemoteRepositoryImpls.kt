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

import com.aura.launcher.data.local.dao.CachedContentDao
import com.aura.launcher.data.local.entities.CachedIconPackEntity
import com.aura.launcher.data.local.entities.CachedThemeEntity
import com.aura.launcher.data.local.entities.CachedWallpaperEntity

class WallpaperRepositoryImpl(
    private val context: Context,
    private val wallpaperApi: WallpaperApi,
    private val cachedContentDao: CachedContentDao? = null
) : WallpaperRepository {

    // Built-in seed data for initial cold-start fallback
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
                if (list.isNotEmpty()) {
                    cachedContentDao?.insertCachedWallpapers(list.map {
                        CachedWallpaperEntity(
                            id = it.id,
                            title = it.title,
                            imageUrl = it.imageUrl,
                            thumbnailUrl = it.thumbnailUrl,
                            category = it.category,
                            tier = it.tier,
                            downloads = it.downloads
                        )
                    })
                    NetworkResult.Success(list)
                } else {
                    fallbackToCache()
                }
            } else {
                fallbackToCache()
            }
        } catch (e: Exception) {
            fallbackToCache()
        }
    }

    private suspend fun fallbackToCache(): NetworkResult<List<RemoteWallpaper>> {
        val cached = cachedContentDao?.getAllCachedWallpapers()
        return if (!cached.isNullOrEmpty()) {
            NetworkResult.Success(cached.map {
                RemoteWallpaper(
                    id = it.id,
                    title = it.title,
                    imageUrl = it.imageUrl,
                    thumbnailUrl = it.thumbnailUrl,
                    category = it.category,
                    tier = it.tier,
                    downloads = it.downloads
                )
            })
        } else {
            NetworkResult.Success(defaultWallpapers)
        }
    }

    override suspend fun getWallpaperById(id: String): NetworkResult<RemoteWallpaper> {
        val found = defaultWallpapers.find { it.id == id }
        return if (found != null) NetworkResult.Success(found) else NetworkResult.Error(404, "Wallpaper not found")
    }
    
        /**
     * Safety guard: while Aura isn't the phone's actual Home app, none of
     * its customization actions should touch the real device — not even
     * the wallpaper. Only true once the user has explicitly granted the
     * Home role (see PackageManagerHelper.openDefaultLauncherSettings()).
     */
    private fun isDefaultLauncher(): Boolean {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
        }
        val resolveInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                intent,
                android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    override suspend fun applyWallpaperBitmap(bitmap: android.graphics.Bitmap): Boolean = withContext(Dispatchers.IO) {
        if (!isDefaultLauncher()) return@withContext false
        try {
            WallpaperManager.getInstance(context).setBitmap(bitmap)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun applyWallpaper(imageUrl: String): Boolean = withContext(Dispatchers.IO) { 
        if (!isDefaultLauncher()) return@withContext false
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
    private val iconPackApi: IconPackApi,
    private val cachedContentDao: CachedContentDao? = null
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
                if (list.isNotEmpty()) {
                    cachedContentDao?.insertCachedIconPacks(list.map {
                        CachedIconPackEntity(
                            id = it.id,
                            name = it.name,
                            description = it.description,
                            iconCount = it.iconCount,
                            previewUrl = it.previewUrl,
                            tier = it.tier,
                            author = it.author
                        )
                    })
                    NetworkResult.Success(list)
                } else {
                    fallbackToCache()
                }
            } else {
                fallbackToCache()
            }
        } catch (e: Exception) {
            fallbackToCache()
        }
    }

    private suspend fun fallbackToCache(): NetworkResult<List<RemoteIconPack>> {
        val cached = cachedContentDao?.getAllCachedIconPacks()
        return if (!cached.isNullOrEmpty()) {
            NetworkResult.Success(cached.map {
                RemoteIconPack(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    iconCount = it.iconCount,
                    previewUrl = it.previewUrl,
                    tier = it.tier,
                    author = it.author
                )
            })
        } else {
            NetworkResult.Success(defaultIconPacks)
        }
    }

    override suspend fun getIconPackById(id: String): NetworkResult<RemoteIconPack> {
        val found = defaultIconPacks.find { it.id == id }
        return if (found != null) NetworkResult.Success(found) else NetworkResult.Error(404, "Icon Pack not found")
    }
}

class ThemeRepositoryImpl(
    private val themeApi: ThemeApi,
    private val cachedContentDao: CachedContentDao? = null
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
                if (list.isNotEmpty()) {
                    cachedContentDao?.insertCachedThemes(list.map {
                        CachedThemeEntity(
                            id = it.id,
                            name = it.name,
                            description = it.description,
                            primaryColor = it.primaryColor,
                            secondaryColor = it.secondaryColor,
                            previewUrl = it.previewUrl,
                            tier = it.tier
                        )
                    })
                    NetworkResult.Success(list)
                } else {
                    fallbackToCache()
                }
            } else {
                fallbackToCache()
            }
        } catch (e: Exception) {
            fallbackToCache()
        }
    }

    private suspend fun fallbackToCache(): NetworkResult<List<RemoteTheme>> {
        val cached = cachedContentDao?.getAllCachedThemes()
        return if (!cached.isNullOrEmpty()) {
            NetworkResult.Success(cached.map {
                RemoteTheme(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    primaryColor = it.primaryColor,
                    secondaryColor = it.secondaryColor,
                    previewUrl = it.previewUrl,
                    tier = it.tier
                )
            })
        } else {
            NetworkResult.Success(defaultThemes)
        }
    }

    override suspend fun getThemeById(id: String): NetworkResult<RemoteTheme> {
        val found = defaultThemes.find { it.id == id }
        return if (found != null) NetworkResult.Success(found) else NetworkResult.Error(404, "Theme not found")
    }
}

