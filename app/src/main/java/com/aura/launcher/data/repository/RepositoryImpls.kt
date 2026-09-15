package com.aura.launcher.data.repository

import com.aura.launcher.core.datastore.AuthPreferences
import com.aura.launcher.core.datastore.LauncherPreferences
import com.aura.launcher.core.utils.PackageManagerHelper
import com.aura.launcher.data.local.dao.*
import com.aura.launcher.data.local.entities.*
import com.aura.launcher.core.security.DeviceCredentialManager
import com.aura.launcher.data.remote.api.AuthApi
import com.aura.launcher.data.remote.api.DeviceAuthApi
import com.aura.launcher.data.remote.dto.DeviceChallengeRequestDto
import com.aura.launcher.data.remote.dto.DeviceRegisterRequestDto
import com.aura.launcher.data.remote.dto.DeviceVerifyResetRequestDto
import com.aura.launcher.data.remote.dto.DeviceVerifyLoginRequestDto
import com.aura.launcher.domain.model.*
import com.aura.launcher.domain.repository.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
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
    private val firebaseAuth: FirebaseAuth,
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
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, passwordHash).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Sign up failed. Please try again."))

            val profileUpdate = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdate).await()

            // The backend provisions the Firestore user document on first
            // authenticated call and returns the canonical profile — the app
            // never writes user profile fields directly to Firestore.
            val user = syncProfileFromBackend(firebaseUser.uid, fallbackEmail = email, fallbackName = name)
            authPreferences.saveSession(firebaseUser.uid, isGuest = false)

            Result.success(user)
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            Result.failure(Exception("An account with this email already exists."))
        } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("Password is too weak. Please choose a stronger one."))
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Please enter a valid email address."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Sign up failed. Please check your connection."))
        }
    }

    override suspend fun login(email: String, passwordHash: String): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, passwordHash).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Login failed. Please try again."))

            val user = syncProfileFromBackend(
                firebaseUser.uid,
                fallbackEmail = email,
                fallbackName = firebaseUser.displayName ?: email.substringBefore("@")
            )
            authPreferences.saveSession(firebaseUser.uid, isGuest = false)

            Result.success(user)
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Account not found. Please sign up."))
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Incorrect email or password."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Login failed. Please check your connection."))
        }
    }

    /**
     * Same finish line as [login] (sync profile, save session) but starting
     * from a Firebase custom token instead of email+password — used after a
     * device-verified forgot-password login (see verify-login.php).
     */
    override suspend fun loginWithCustomToken(signInToken: String): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithCustomToken(signInToken).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Sign-in failed. Please try again."))

            val user = syncProfileFromBackend(
                firebaseUser.uid,
                fallbackEmail = firebaseUser.email ?: "",
                fallbackName = firebaseUser.displayName ?: (firebaseUser.email ?: "").substringBefore("@")
            )
            authPreferences.saveSession(firebaseUser.uid, isGuest = false)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Sign-in failed. Please check your connection."))
        }
    }

    /**
     * Calls GET /auth/me (backend verifies the Firebase ID token attached by
     * AuthInterceptor) to fetch/provision the canonical profile, then caches
     * it locally for offline access. Falls back to Firebase-only info if the
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
            // Offline or backend down — Firebase-only defaults above already apply.
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
        // Guest mode stays fully local/offline — no Firebase account is created.
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
        firebaseAuth.signOut()
        userDao.clearCurrentSessions()
        authPreferences.clearSession()
    }
}

/**
 * Secure device authentication for password reset — see
 * domain.repository.DeviceAuthRepository for the full contract. This class
 * never decides trust locally: [checkDeviceTrust] and
 * [verifyDeviceAndResetPassword] both defer entirely to the backend.
 */
class DeviceAuthRepositoryImpl(
    private val deviceCredentialManager: DeviceCredentialManager,
    private val trustedDeviceDao: TrustedDeviceDao,
    private val deviceAuthApi: DeviceAuthApi,
    private val firebaseAuth: FirebaseAuth
) : DeviceAuthRepository {

    override fun getDeviceSecurityCapability(): DeviceSecurityCapability =
        deviceCredentialManager.getDeviceSecurityCapability()

    override suspend fun hasLocalDeviceKey(userId: String): Boolean {
        val entity = trustedDeviceDao.getForUser(userId)
        return entity != null && entity.isActive && deviceCredentialManager.hasKey(userId)
    }

    override suspend fun registerDevice(userId: String, email: String, deviceLabel: String): Result<Unit> {
        return try {
            val deviceId = deviceCredentialManager.getOrCreateDeviceId()
            val publicKeyBase64 = deviceCredentialManager.generateDeviceKeyPair(userId)

            val response = deviceAuthApi.registerDevice(
                DeviceRegisterRequestDto(
                    deviceId = deviceId,
                    publicKey = publicKeyBase64,
                    deviceLabel = deviceLabel
                )
            )

            if (response.isSuccessful && response.body()?.registered == true) {
                trustedDeviceDao.insertOrUpdate(
                    TrustedDeviceEntity(
                        userId = userId,
                        email = email,
                        deviceId = deviceId,
                        keyAlias = "aura_device_key_$userId",
                        deviceLabel = deviceLabel
                    )
                )
                Result.success(Unit)
            } else {
                // Registration didn't take on the backend — don't leave an
                // orphaned local key claiming this device is trusted.
                deviceCredentialManager.deleteKey(userId)
                Result.failure(Exception("Couldn't register this device. Please try again."))
            }
        } catch (e: Exception) {
            deviceCredentialManager.deleteKey(userId)
            Result.failure(Exception("Couldn't register this device. Please check your connection."))
        }
    }

    override suspend fun forgetDevice(userId: String) {
        deviceCredentialManager.deleteKey(userId)
        trustedDeviceDao.deleteForUser(userId)
    }

    override suspend fun getLocalUserIdForEmail(email: String): String? =
        trustedDeviceDao.getByEmail(email)?.takeIf { it.isActive }?.userId

    override suspend fun getAllTrustedAccounts(): List<TrustedAccountSummary> =
        trustedDeviceDao.getAll().map {
            TrustedAccountSummary(userId = it.userId, email = it.email, deviceLabel = it.deviceLabel)
        }

    override suspend fun checkDeviceTrust(email: String): Result<DeviceTrustCheck> {
        return try {
            val deviceId = deviceCredentialManager.getOrCreateDeviceId()
            val response = deviceAuthApi.requestChallenge(
                DeviceChallengeRequestDto(email = email, deviceId = deviceId)
            )
            val body = response.body()
            if (response.isSuccessful && body != null && body.deviceTrusted &&
                body.challenge != null && body.deviceId != null
            ) {
                Result.success(DeviceTrustCheck.Trusted(body.deviceId, body.challenge))
            } else {
                Result.success(DeviceTrustCheck.NotTrusted)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Couldn't reach the server. Please check your connection."))
        }
    }

    override suspend fun verifyDeviceAndResetPassword(
        email: String,
        deviceId: String,
        challenge: String,
        signatureBase64: String,
        newPassword: String
    ): Result<DeviceVerificationResult> {
        return try {
            val response = deviceAuthApi.verifyAndResetPassword(
                DeviceVerifyResetRequestDto(
                    email = email,
                    deviceId = deviceId,
                    challenge = challenge,
                    signature = signatureBase64,
                    newPassword = newPassword
                )
            )
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                Result.success(DeviceVerificationResult.Success(body.signInToken))
            } else {
                Result.success(
                    DeviceVerificationResult.Failed(
                        body?.message ?: "Device verification was unsuccessful. Please try again."
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Couldn't reach the server. Please check your connection."))
        }
    }

    /**
     * Signature-only login used by forgot-password once biometric succeeds:
     * no password field, a valid signature just returns a sign-in token.
     */
    override suspend fun verifyDeviceAndLogin(
        email: String,
        deviceId: String,
        challenge: String,
        signatureBase64: String
    ): Result<DeviceLoginResult> {
        return try {
            val response = deviceAuthApi.verifyAndLogin(
                DeviceVerifyLoginRequestDto(
                    email = email,
                    deviceId = deviceId,
                    challenge = challenge,
                    signature = signatureBase64
                )
            )
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.signInToken != null) {
                Result.success(DeviceLoginResult.Success(body.signInToken))
            } else {
                Result.success(
                    DeviceLoginResult.Failed(
                        body?.message ?: "Device verification was unsuccessful. Please try again."
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Couldn't reach the server. Please check your connection."))
        }
    }

    override suspend fun sendFallbackResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Couldn't send the reset email. Please check the address and try again."))
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
