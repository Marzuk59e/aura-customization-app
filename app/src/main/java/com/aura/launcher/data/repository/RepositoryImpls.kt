package com.aura.launcher.data.repository

import com.aura.launcher.core.datastore.AuthPreferences
import com.aura.launcher.core.datastore.LauncherPreferences
import com.aura.launcher.core.utils.PackageManagerHelper
import com.aura.launcher.data.local.dao.*
import com.aura.launcher.data.local.entities.*
import com.aura.launcher.domain.model.*
import com.aura.launcher.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AppRepositoryImpl(
    private val appDao: AppDao,
    private val packageManagerHelper: PackageManagerHelper
) : AppRepository {

    override fun getAllApps(): Flow<List<AppInfo>> {
        return appDao.getAllApps().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchApps(query: String): Flow<List<AppInfo>> {
        return appDao.searchApps(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshInstalledApps() {
        val installed = packageManagerHelper.getInstalledLaunchableApps()
        val entities = installed.map { it.toEntity() }
        appDao.insertApps(entities)
    }

    override suspend fun removeAppByPackage(packageName: String) {
        appDao.deleteByPackage(packageName)
    }

    override suspend fun launchApp(packageName: String, activityName: String?): Boolean {
        val success = packageManagerHelper.launchApp(packageName, activityName)
        if (success && activityName != null) {
            appDao.incrementLaunchCount("$packageName/$activityName")
        }
        return success
    }
}

class HomeRepositoryImpl(
    private val homeItemDao: HomeItemDao,
    private val launcherPreferences: LauncherPreferences
) : HomeRepository {

    override fun getItemsForPage(pageIndex: Int): Flow<List<HomeItem>> {
        return homeItemDao.getItemsForPage(pageIndex).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getDockItems(): Flow<List<HomeItem>> {
        return homeItemDao.getDockItems().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllHomeItems(): Flow<List<HomeItem>> {
        return homeItemDao.getAllHomeItems().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addHomeItem(item: HomeItem): Long {
        return homeItemDao.insertItem(item.toEntity())
    }

    override suspend fun updateHomeItem(item: HomeItem) {
        homeItemDao.updateItem(item.toEntity())
    }

    override suspend fun removeHomeItem(id: Long) {
        homeItemDao.deleteItemById(id)
    }

    override suspend fun clearAndReplaceAll(items: List<HomeItem>) {
        homeItemDao.clearAllHomeItems()
        homeItemDao.insertItems(items.map { it.toEntity() })
    }

    override suspend fun initializeDefaultLayout(installedApps: List<AppInfo>) {
        val isInitialized = launcherPreferences.isInitializedFlow.first()
        if (isInitialized) return

        val defaultDock = installedApps.take(4).mapIndexed { index, app ->
            HomeItem(
                pageIndex = -1,
                cellX = index,
                cellY = 0,
                itemType = HomeItemType.APP,
                packageName = app.packageName,
                activityName = app.activityName,
                label = app.label
            )
        }

        val defaultHome = installedApps.drop(4).take(8).mapIndexed { index, app ->
            val cellX = index % 4
            val cellY = index / 4
            HomeItem(
                pageIndex = 0,
                cellX = cellX,
                cellY = cellY,
                itemType = HomeItemType.APP,
                packageName = app.packageName,
                activityName = app.activityName,
                label = app.label
            )
        }

        homeItemDao.insertItems((defaultDock + defaultHome).map { it.toEntity() })
        launcherPreferences.setInitialized(true)
    }
}

class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val authPreferences: AuthPreferences
) : AuthRepository {

    override val currentUserFlow: Flow<User?> = userDao.getCurrentUser().map { it?.toDomain() }

    override suspend fun getCurrentUser(): User? {
        return userDao.getCurrentUserDirect()?.toDomain()
    }

    override suspend fun signUp(email: String, name: String, passwordHash: String): Result<User> {
        val existing = userDao.getUserByEmail(email)
        if (existing != null) {
            return Result.failure(Exception("An account with this email already exists."))
        }
        val userId = UUID.randomUUID().toString()
        val userEntity = UserEntity(
            id = userId,
            email = email,
            displayName = name,
            avatarUrl = null,
            isGuest = false,
            isCurrentLoggedIn = true
        )
        userDao.clearCurrentSessions()
        userDao.insertOrUpdateUser(userEntity)
        authPreferences.saveSession(userId, isGuest = false)
        return Result.success(userEntity.toDomain())
    }

    override suspend fun login(email: String, passwordHash: String): Result<User> {
        val existing = userDao.getUserByEmail(email)
            ?: return Result.failure(Exception("Account not found. Please sign up."))
        
        userDao.clearCurrentSessions()
        userDao.setCurrentSession(existing.id)
        authPreferences.saveSession(existing.id, isGuest = false)
        return Result.success(existing.toDomain())
    }

    override suspend fun continueAsGuest(): User {
        val guestId = "guest_" + UUID.randomUUID().toString().take(8)
        val guestEntity = UserEntity(
            id = guestId,
            email = "guest@auralauncher.local",
            displayName = "Aura Guest",
            isGuest = true,
            isCurrentLoggedIn = true
        )
        userDao.clearCurrentSessions()
        userDao.insertOrUpdateUser(guestEntity)
        authPreferences.saveSession(guestId, isGuest = true)
        return guestEntity.toDomain()
    }

    override suspend fun logout() {
        userDao.clearCurrentSessions()
        authPreferences.clearSession()
    }
}

class SavedSetupRepositoryImpl(
    private val savedSetupDao: SavedSetupDao,
    private val favoriteDao: FavoriteDao,
    private val homeItemDao: HomeItemDao
) : SavedSetupRepository {

    override fun getSetupsForUser(userId: String): Flow<List<SavedSetup>> {
        return savedSetupDao.getSetupsForUser(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveCurrentSetup(
        userId: String,
        title: String,
        description: String,
        previewWallpaper: String,
        iconPackName: String,
        gridRows: Int,
        gridCols: Int,
        items: List<HomeItem>
    ): Long {
        val jsonArray = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("pageIndex", item.pageIndex)
                put("cellX", item.cellX)
                put("cellY", item.cellY)
                put("spanX", item.spanX)
                put("spanY", item.spanY)
                put("itemType", item.itemType.name)
                put("packageName", item.packageName)
                put("activityName", item.activityName)
                put("label", item.label)
            }
            jsonArray.put(obj)
        }

        val entity = SavedSetupEntity(
            userId = userId,
            title = title,
            description = description,
            previewWallpaper = previewWallpaper,
            iconPackName = iconPackName,
            gridRows = gridRows,
            gridCols = gridCols,
            layoutJson = jsonArray.toString()
        )
        return savedSetupDao.insertSetup(entity)
    }

    override suspend fun applySavedSetup(setupId: Long, homeRepository: HomeRepository): Boolean {
        val setup = savedSetupDao.getSetupById(setupId) ?: return false
        val items = mutableListOf<HomeItem>()
        val jsonArray = JSONArray(setup.layoutJson)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            items.add(
                HomeItem(
                    pageIndex = obj.getInt("pageIndex"),
                    cellX = obj.getInt("cellX"),
                    cellY = obj.getInt("cellY"),
                    spanX = obj.optInt("spanX", 1),
                    spanY = obj.optInt("spanY", 1),
                    itemType = HomeItemType.valueOf(obj.optString("itemType", "APP")),
                    packageName = obj.optString("packageName", null),
                    activityName = obj.optString("activityName", null),
                    label = obj.optString("label", null)
                )
            )
        }
        homeRepository.clearAndReplaceAll(items)
        return true
    }

    override suspend fun deleteSetup(setup: SavedSetup) {
        savedSetupDao.deleteSetup(setup.toEntity())
    }

    override suspend fun toggleSetupFavorite(setupId: Long, isFavorite: Boolean) {
        savedSetupDao.toggleFavorite(setupId, isFavorite)
    }

    override fun getFavoritesForUser(userId: String): Flow<List<FavoriteItem>> {
        return favoriteDao.getFavoritesForUser(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun toggleFavorite(
        userId: String,
        type: FavoriteType,
        targetId: String,
        title: String,
        previewUrl: String
    ): Boolean {
        val isFav = favoriteDao.isFavorited(userId, targetId, type.name)
        if (isFav) {
            favoriteDao.removeFavorite(userId, targetId, type.name)
            return false
        } else {
            favoriteDao.addFavorite(
                FavoriteEntity(
                    userId = userId,
                    type = type.name,
                    targetId = targetId,
                    title = title,
                    previewUrl = previewUrl
                )
            )
            return true
        }
    }
}

class SettingsRepositoryImpl(
    private val launcherPreferences: LauncherPreferences,
    private val packageManagerHelper: PackageManagerHelper
) : SettingsRepository {

    override val settingsFlow: Flow<LauncherSettings> = launcherPreferences.settingsFlow

    override suspend fun updateGridDimensions(rows: Int, cols: Int) {
        launcherPreferences.updateGrid(rows, cols)
    }

    override suspend fun updateIconScale(scale: Float) {
        launcherPreferences.updateIconScale(scale)
    }

    override suspend fun toggleLabels(show: Boolean) {
        launcherPreferences.updateShowLabels(show)
    }

    override suspend fun toggleDarkTheme(dark: Boolean) {
        launcherPreferences.updateTheme(dark)
    }

    override fun isDefaultLauncher(): Boolean {
        return packageManagerHelper.isDefaultLauncher()
    }

    override fun requestDefaultLauncher() {
        packageManagerHelper.openDefaultLauncherSettings()
    }

    override suspend fun saveSelectedIconPack(name: String) {
        launcherPreferences.setSelectedIconPack(name)
    }

    override suspend fun saveSelectedTheme(themeId: String, primaryColor: String, secondaryColor: String) {
        launcherPreferences.setSelectedTheme(themeId, primaryColor, secondaryColor)
    }

    override suspend fun saveSelectedFont(fontName: String) {
        launcherPreferences.setSelectedFont(fontName)
    }
}


// Extension mappers
fun AppEntity.toDomain() = AppInfo(
    packageName = packageName,
    activityName = activityName,
    label = label,
    category = category,
    isHidden = isHidden,
    installTime = installTime,
    launchCount = launchCount
)

fun AppInfo.toEntity() = AppEntity(
    componentKey = componentKey,
    packageName = packageName,
    activityName = activityName,
    label = label,
    category = category,
    isHidden = isHidden,
    installTime = installTime,
    launchCount = launchCount
)

fun HomeItemEntity.toDomain() = HomeItem(
    id = id,
    pageIndex = pageIndex,
    cellX = cellX,
    cellY = cellY,
    spanX = spanX,
    spanY = spanY,
    itemType = HomeItemType.valueOf(itemType),
    packageName = packageName,
    activityName = activityName,
    label = label,
    iconUri = iconUri,
    folderId = folderId,
    widgetId = widgetId,
    widgetProvider = widgetProvider
)

fun HomeItem.toEntity() = HomeItemEntity(
    id = id,
    pageIndex = pageIndex,
    cellX = cellX,
    cellY = cellY,
    spanX = spanX,
    spanY = spanY,
    itemType = itemType.name,
    packageName = packageName,
    activityName = activityName,
    label = label,
    iconUri = iconUri,
    folderId = folderId,
    widgetId = widgetId,
    widgetProvider = widgetProvider
)

fun UserEntity.toDomain() = User(
    id = id,
    email = email,
    displayName = displayName,
    avatarUrl = avatarUrl,
    isGuest = isGuest,
    createdAt = createdAt
)

fun SavedSetupEntity.toDomain() = SavedSetup(
    id = id,
    userId = userId,
    title = title,
    description = description,
    previewWallpaper = previewWallpaper,
    iconPackName = iconPackName,
    gridRows = gridRows,
    gridCols = gridCols,
    layoutJson = layoutJson,
    createdAt = createdAt,
    isFavorite = isFavorite
)

fun SavedSetup.toEntity() = SavedSetupEntity(
    id = id,
    userId = userId,
    title = title,
    description = description,
    previewWallpaper = previewWallpaper,
    iconPackName = iconPackName,
    gridRows = gridRows,
    gridCols = gridCols,
    layoutJson = layoutJson,
    createdAt = createdAt,
    isFavorite = isFavorite
)

fun FavoriteEntity.toDomain() = FavoriteItem(
    id = id,
    userId = userId,
    type = FavoriteType.valueOf(type),
    targetId = targetId,
    title = title,
    previewUrl = previewUrl,
    addedAt = addedAt
)
