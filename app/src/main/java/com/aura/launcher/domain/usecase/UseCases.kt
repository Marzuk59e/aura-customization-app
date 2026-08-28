package com.aura.launcher.domain.usecase

import com.aura.launcher.domain.model.*
import com.aura.launcher.domain.repository.*
import kotlinx.coroutines.flow.Flow

class GetInstalledAppsUseCase(private val appRepository: AppRepository) {
    operator fun invoke(): Flow<List<AppInfo>> = appRepository.getAllApps()
    fun search(query: String): Flow<List<AppInfo>> = appRepository.searchApps(query)
    suspend fun refresh() = appRepository.refreshInstalledApps()
}

class LaunchAppUseCase(private val appRepository: AppRepository) {
    suspend operator fun invoke(packageName: String, activityName: String? = null): Boolean {
        return appRepository.launchApp(packageName, activityName)
    }
}

class ManageHomeItemsUseCase(
    private val homeRepository: HomeRepository,
    private val appRepository: AppRepository
) {
    fun getPageItems(pageIndex: Int): Flow<List<HomeItem>> = homeRepository.getItemsForPage(pageIndex)
    fun getDockItems(): Flow<List<HomeItem>> = homeRepository.getDockItems()
    fun getAllItems(): Flow<List<HomeItem>> = homeRepository.getAllHomeItems()

    suspend fun addItem(item: HomeItem): Long = homeRepository.addHomeItem(item)
    suspend fun updateItem(item: HomeItem) = homeRepository.updateHomeItem(item)
    suspend fun removeItem(id: Long) = homeRepository.removeHomeItem(id)
    suspend fun initializeDefault(installedApps: List<AppInfo>) {
        homeRepository.initializeDefaultLayout(installedApps)
    }
}

class AuthUseCase(private val authRepository: AuthRepository) {
    val currentUser: Flow<User?> = authRepository.currentUserFlow
    suspend fun getCurrentUser(): User? = authRepository.getCurrentUser()
    suspend fun signUp(email: String, name: String, password: String): Result<User> =
        authRepository.signUp(email, name, password)
    suspend fun login(email: String, password: String): Result<User> =
        authRepository.login(email, password)
    suspend fun continueAsGuest(): User = authRepository.continueAsGuest()
    suspend fun logout() = authRepository.logout()
}

class ManageSavedSetupsUseCase(
    private val savedSetupRepository: SavedSetupRepository,
    private val homeRepository: HomeRepository
) {
    fun getUserSetups(userId: String): Flow<List<SavedSetup>> =
        savedSetupRepository.getSetupsForUser(userId)

    suspend fun saveCurrentSetup(
        userId: String,
        title: String,
        description: String,
        previewWallpaper: String,
        iconPackName: String,
        gridRows: Int,
        gridCols: Int,
        items: List<HomeItem>
    ): Long = savedSetupRepository.saveCurrentSetup(
        userId, title, description, previewWallpaper, iconPackName, gridRows, gridCols, items
    )

    suspend fun applySetup(setupId: Long): Boolean =
        savedSetupRepository.applySavedSetup(setupId, homeRepository)

    suspend fun deleteSetup(setup: SavedSetup) =
        savedSetupRepository.deleteSetup(setup)

    suspend fun toggleSetupFavorite(setupId: Long, isFavorite: Boolean) =
        savedSetupRepository.toggleSetupFavorite(setupId, isFavorite)

    fun getUserFavorites(userId: String): Flow<List<FavoriteItem>> =
        savedSetupRepository.getFavoritesForUser(userId)

    suspend fun toggleFavorite(
        userId: String,
        type: FavoriteType,
        targetId: String,
        title: String,
        previewUrl: String
    ): Boolean = savedSetupRepository.toggleFavorite(userId, type, targetId, title, previewUrl)
}
