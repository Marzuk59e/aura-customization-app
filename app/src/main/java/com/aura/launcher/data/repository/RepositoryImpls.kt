package com.aura.launcher.data.repository

import com.aura.launcher.core.datastore.AuthPreferences
import com.aura.launcher.core.datastore.LauncherPreferences
import com.aura.launcher.core.utils.PackageManagerHelper
import com.aura.launcher.data.local.dao.*
import com.aura.launcher.data.local.entities.*
import com.aura.launcher.data.remote.api.AuthApi
import com.aura.launcher.data.remote.api.SupabaseAuthApi
import com.aura.launcher.data.remote.dto.SupabaseErrorResponseDto
import com.aura.launcher.data.remote.dto.SupabasePasswordGrantRequestDto
import com.aura.launcher.data.remote.dto.SupabaseRecoverRequestDto
import com.aura.launcher.data.remote.dto.SupabaseSignUpRequestDto
import com.aura.launcher.data.remote.dto.SupabaseUpdateUserRequestDto
import com.aura.launcher.auth.PasswordRecoveryLinkHolder
import com.aura.launcher.domain.model.*
import com.aura.launcher.domain.repository.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
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
    private val launcherPreferences: LauncherPreferences,
    private val folderDao: FolderDao? = null,
    private val appDao: AppDao? = null
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

    override suspend fun getFolder(folderId: Long): Folder? {
        val entity = folderDao?.getFolderById(folderId) ?: return null
        val appKeys = try {
            val arr = JSONArray(entity.appKeysJson)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }

        val apps = mutableListOf<AppInfo>()
        for (key in appKeys) {
            val appEntity = appDao?.getAppByKey(key)
            if (appEntity != null) {
                apps.add(appEntity.toDomain())
            }
        }

        return Folder(
            id = entity.id,
            title = entity.title,
            color = entity.color,
            apps = apps
        )
    }

    override suspend fun createFolder(title: String, color: String, appKeys: List<String>): Long {
        val json = JSONArray(appKeys).toString()
        val entity = FolderEntity(
            title = title,
            color = color,
            appKeysJson = json
        )
        return folderDao?.insertFolder(entity) ?: 0L
    }

    override suspend fun updateFolder(folderId: Long, title: String, appKeys: List<String>) {
        val json = JSONArray(appKeys).toString()
        val entity = FolderEntity(
            id = folderId,
            title = title,
            appKeysJson = json
        )
        folderDao?.updateFolder(entity)
    }

    override suspend fun deleteFolder(folderId: Long) {
        val entity = folderDao?.getFolderById(folderId)
        if (entity != null) {
            folderDao.deleteFolder(entity)
        }
    }
}

class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val authPreferences: AuthPreferences,
    private val supabaseAuthApi: SupabaseAuthApi,
    private val authApi: AuthApi
) : AuthRepository {

    // aura_launcher.db is included in Android's cloud-backup / device-transfer rules
    // (see xml/backup_rules.xml), so the Room row alone isn't a safe signal after a
    // fresh install: a restored backup could bring back a "logged in" user row even
    // though the person just reinstalled the app. authPreferences (DataStore) is NOT
    // included in those backup rules, so we only trust a session when both agree.
    override val currentUserFlow: Flow<User?> = combine(
        userDao.getCurrentUser(),
        authPreferences.activeUserIdFlow
    ) { userEntity, activeUserId ->
        if (userEntity != null && activeUserId != null && userEntity.id == activeUserId) {
            userEntity.toDomain()
        } else {
            null
        }
    }

    override suspend fun getCurrentUser(): User? {
        return userDao.getCurrentUserDirect()?.toDomain()
    }

    override suspend fun signUp(email: String, name: String, passwordHash: String): Result<User> {
        return try {
            val response = supabaseAuthApi.signUp(
                SupabaseSignUpRequestDto(
                    email = email,
                    password = passwordHash,
                    data = mapOf("full_name" to name)
                )
            )
            val body = response.body()

            if (!response.isSuccessful || body?.user == null) {
                return Result.failure(Exception(friendlySupabaseError(response, isSignUp = true)))
            }

            // If the Supabase project requires email confirmation, the user
            // row exists but no session is issued yet (accessToken is null).
            // That's a real, different outcome from "signed in" — surface it
            // instead of quietly treating it as success.
            if (body.accessToken == null || body.refreshToken == null) {
                return Result.failure(
                    Exception("Account created. Please check your email to confirm before logging in.")
                )
            }

            authPreferences.saveSupabaseTokens(body.accessToken, body.refreshToken, body.expiresIn ?: 3600L)

            val supabaseUserId = body.user.id
            // The backend provisions the user's profile row on first
            // authenticated call and returns the canonical profile — the app
            // never writes user profile fields directly.
            val user = syncProfileFromBackend(supabaseUserId, fallbackEmail = email, fallbackName = name)
            authPreferences.saveSession(supabaseUserId, isGuest = false)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Sign up failed. Please check your connection."))
        }
    }

    override suspend fun login(email: String, passwordHash: String): Result<User> {
        return try {
            val response = supabaseAuthApi.signInWithPassword(
                body = SupabasePasswordGrantRequestDto(email = email, password = passwordHash)
            )
            val body = response.body()

            if (!response.isSuccessful || body?.accessToken == null || body.refreshToken == null || body.user == null) {
                return Result.failure(Exception(friendlySupabaseError(response, isSignUp = false)))
            }

            authPreferences.saveSupabaseTokens(body.accessToken, body.refreshToken, body.expiresIn ?: 3600L)

            val supabaseUserId = body.user.id
            @Suppress("UNCHECKED_CAST")
            val metadataName = (body.user.userMetadata?.get("full_name") as? String)
            val user = syncProfileFromBackend(
                supabaseUserId,
                fallbackEmail = body.user.email ?: email,
                fallbackName = metadataName ?: email.substringBefore("@")
            )
            authPreferences.saveSession(supabaseUserId, isGuest = false)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Login failed. Please check your connection."))
        }
    }

    /** Reads whichever error field GoTrue actually sent and maps it to user-facing copy. */
    private fun friendlySupabaseError(response: Response<*>, isSignUp: Boolean): String {
        val raw = try {
            val errorBody = response.errorBody()?.string()
            if (errorBody.isNullOrBlank()) null
            else {
                val parsed = Gson().fromJson(errorBody, SupabaseErrorResponseDto::class.java)
                parsed.msg ?: parsed.errorDescription ?: parsed.error
            }
        } catch (e: Exception) {
            null
        }

        val lower = raw?.lowercase() ?: ""
        return when {
            "already registered" in lower || "already exists" in lower ->
                "An account with this email already exists."
            "password" in lower && ("weak" in lower || "short" in lower || "least" in lower) ->
                "Password is too weak. Please choose a stronger one."
            "invalid login credentials" in lower || "invalid_credentials" in lower ->
                "Incorrect email or password."
            "email" in lower && "invalid" in lower ->
                "Please enter a valid email address."
            !raw.isNullOrBlank() -> raw
            isSignUp -> "Sign up failed. Please try again."
            else -> "Login failed. Please try again."
        }
    }

    /**
     * Calls GET /auth/me (backend verifies the Supabase access token attached by
     * AuthInterceptor) to fetch/provision the canonical profile, then caches
     * it locally for offline access. Falls back to the login response's info if the
     * backend is unreachable, so login still works offline after the first
     * successful sync.
     */
    private suspend fun syncProfileFromBackend(uid: String, fallbackEmail: String, fallbackName: String): User {
        var name: String = fallbackName
        var email: String = fallbackEmail
        var avatarUrl: String? = null
        try {
            val response = authApi.me()
            val profile = if (response.isSuccessful) response.body() else null
            name = profile?.name ?: fallbackName
            email = profile?.email ?: fallbackEmail
            avatarUrl = profile?.photoUrl
        } catch (e: Exception) {
            // Offline or backend down — the fallback name/email above already apply.
        }

        val userEntity = UserEntity(
            id = uid,
            email = email,
            displayName = name,
            avatarUrl = avatarUrl,
            isGuest = false,
            isCurrentLoggedIn = true
        )
        userDao.clearCurrentSessions()
        userDao.insertOrUpdateUser(userEntity)
        return userEntity.toDomain()
    }

    override suspend fun continueAsGuest(): User {
        // Guest mode stays fully local/offline — no Supabase account is created.
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
        // Client-side logout only: clears the local Supabase session.
        // Revoking the refresh token server-side needs Authorization: Bearer
        // <user's access token>, which AuthInterceptor doesn't send yet —
        // that lands in Phase 3 alongside the rest of the interceptor rework.
        userDao.clearCurrentSessions()
        authPreferences.clearSession() // also clears Supabase access/refresh tokens
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> {
        return try {
            val response = supabaseAuthApi.recover(SupabaseRecoverRequestDto(email = email.trim()))
            // GoTrue returns 200/204 whether or not the email exists, by
            // design (so this never leaks which emails have accounts).
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(friendlySupabaseError(response, isSignUp = false)))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Couldn't send the reset email. Please check your connection."))
        }
    }

    override suspend fun confirmPasswordReset(newPassword: String): Result<Unit> {
        val link = PasswordRecoveryLinkHolder.current.value
            ?: return Result.failure(Exception("This reset link has expired. Please request a new one."))

        return try {
            val response = supabaseAuthApi.updateUser(
                authorization = "Bearer ${link.accessToken}",
                body = SupabaseUpdateUserRequestDto(password = newPassword)
            )
            if (response.isSuccessful) {
                PasswordRecoveryLinkHolder.consume()
                Result.success(Unit)
            } else {
                Result.failure(Exception(friendlySupabaseError(response, isSignUp = false)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Couldn't reset the password. Please check your connection."))
        }
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
