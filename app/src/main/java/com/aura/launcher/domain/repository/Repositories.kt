package com.aura.launcher.domain.repository

import com.aura.launcher.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getAllApps(): Flow<List<AppInfo>>
    fun searchApps(query: String): Flow<List<AppInfo>>
    suspend fun refreshInstalledApps()
    suspend fun removeAppByPackage(packageName: String)
    suspend fun launchApp(packageName: String, activityName: String?): Boolean
}

interface HomeRepository {
    fun getItemsForPage(pageIndex: Int): Flow<List<HomeItem>>
    fun getDockItems(): Flow<List<HomeItem>>
    fun getAllHomeItems(): Flow<List<HomeItem>>
    suspend fun addHomeItem(item: HomeItem): Long
    suspend fun updateHomeItem(item: HomeItem)
    suspend fun removeHomeItem(id: Long)
    suspend fun clearAndReplaceAll(items: List<HomeItem>)
    suspend fun initializeDefaultLayout(installedApps: List<AppInfo>)
}

interface AuthRepository {
    val currentUserFlow: Flow<User?>
    suspend fun getCurrentUser(): User?
    suspend fun signUp(email: String, name: String, passwordHash: String): Result<User>
    suspend fun login(email: String, passwordHash: String): Result<User>
    suspend fun continueAsGuest(): User
    suspend fun logout()
}

interface SavedSetupRepository {
    fun getSetupsForUser(userId: String): Flow<List<SavedSetup>>
    suspend fun saveCurrentSetup(
        userId: String,
        title: String,
        description: String,
        previewWallpaper: String,
        iconPackName: String,
        gridRows: Int,
        gridCols: Int,
        items: List<HomeItem>
    ): Long
    suspend fun applySavedSetup(setupId: Long, homeRepository: HomeRepository): Boolean
    suspend fun deleteSetup(setup: SavedSetup)
    suspend fun toggleSetupFavorite(setupId: Long, isFavorite: Boolean)
    fun getFavoritesForUser(userId: String): Flow<List<FavoriteItem>>
    suspend fun toggleFavorite(userId: String, type: FavoriteType, targetId: String, title: String, previewUrl: String): Boolean
}

interface SettingsRepository {
    val settingsFlow: Flow<LauncherSettings>
    suspend fun updateGridDimensions(rows: Int, cols: Int)
    suspend fun updateIconScale(scale: Float)
    suspend fun toggleLabels(show: Boolean)
    suspend fun toggleDarkTheme(dark: Boolean)
    fun isDefaultLauncher(): Boolean
    fun requestDefaultLauncher()

    // New persistence for customization selections
    suspend fun saveSelectedIconPack(name: String)
    suspend fun saveSelectedTheme(themeId: String, primaryColor: String, secondaryColor: String)
    suspend fun saveSelectedFont(fontName: String)
}
