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

    suspend fun getFolder(folderId: Long): Folder? = homeRepository.getFolder(folderId)
    suspend fun createFolder(title: String, color: String, appKeys: List<String>): Long =
        homeRepository.createFolder(title, color, appKeys)
    suspend fun updateFolder(folderId: Long, title: String, appKeys: List<String>) =
        homeRepository.updateFolder(folderId, title, appKeys)
    suspend fun deleteFolder(folderId: Long) = homeRepository.deleteFolder(folderId)
}

class AuthUseCase(private val authRepository: AuthRepository) {
    val currentUser: Flow<User?> = authRepository.currentUserFlow
    suspend fun getCurrentUser(): User? = authRepository.getCurrentUser()
    suspend fun signUp(email: String, name: String, password: String): Result<User> =
        authRepository.signUp(email, name, password)
    suspend fun login(email: String, password: String): Result<User> =
        authRepository.login(email, password)
    suspend fun loginWithCustomToken(signInToken: String): Result<User> =
        authRepository.loginWithCustomToken(signInToken)
    suspend fun continueAsGuest(): User = authRepository.continueAsGuest()
    suspend fun logout() = authRepository.logout()
}

class DeviceAuthUseCase(private val deviceAuthRepository: DeviceAuthRepository) {
    fun getDeviceSecurityCapability(): DeviceSecurityCapability =
        deviceAuthRepository.getDeviceSecurityCapability()

    suspend fun hasLocalDeviceKey(userId: String): Boolean =
        deviceAuthRepository.hasLocalDeviceKey(userId)

    suspend fun registerDevice(userId: String, email: String, deviceLabel: String): Result<Unit> =
        deviceAuthRepository.registerDevice(userId, email, deviceLabel)

    suspend fun forgetDevice(userId: String) = deviceAuthRepository.forgetDevice(userId)

    suspend fun getLocalUserIdForEmail(email: String): String? =
        deviceAuthRepository.getLocalUserIdForEmail(email)

    suspend fun getAllTrustedAccounts(): List<TrustedAccountSummary> =
        deviceAuthRepository.getAllTrustedAccounts()

    suspend fun checkDeviceTrust(email: String): Result<DeviceTrustCheck> =
        deviceAuthRepository.checkDeviceTrust(email)

    suspend fun verifyDeviceAndResetPassword(
        email: String,
        deviceId: String,
        challenge: String,
        signatureBase64: String,
        newPassword: String
    ): Result<DeviceVerificationResult> = deviceAuthRepository.verifyDeviceAndResetPassword(
        email, deviceId, challenge, signatureBase64, newPassword
    )

    suspend fun verifyDeviceAndLogin(
        email: String,
        deviceId: String,
        challenge: String,
        signatureBase64: String
    ): Result<DeviceLoginResult> = deviceAuthRepository.verifyDeviceAndLogin(
        email, deviceId, challenge, signatureBase64
    )

    suspend fun sendFallbackResetEmail(email: String): Result<Unit> =
        deviceAuthRepository.sendFallbackResetEmail(email)
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
