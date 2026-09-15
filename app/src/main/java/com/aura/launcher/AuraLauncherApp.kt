package com.aura.launcher

import android.app.Application
import com.aura.launcher.core.audio.SoundEngine
import com.aura.launcher.core.database.AppDatabase
import com.aura.launcher.core.datastore.AuthPreferences
import com.aura.launcher.core.datastore.LauncherPreferences
import com.aura.launcher.core.network.ApiClient
import com.aura.launcher.core.security.DeviceCredentialManager
import com.aura.launcher.core.utils.PackageManagerHelper
import com.aura.launcher.data.remote.api.*
import com.aura.launcher.data.repository.*
import com.aura.launcher.domain.repository.*
import com.aura.launcher.domain.usecase.*

import com.aura.launcher.receiver.AppChangeCallbackManager
import com.google.firebase.auth.FirebaseAuth

class AuraLauncherApp : Application() {

    lateinit var database: AppDatabase private set
    lateinit var packageManagerHelper: PackageManagerHelper private set
    lateinit var launcherPreferences: LauncherPreferences private set
    lateinit var authPreferences: AuthPreferences private set
    lateinit var appChangeCallbackManager: AppChangeCallbackManager private set
    lateinit var appWidgetHost: com.aura.launcher.launcher.widget.AuraAppWidgetHost private set
    lateinit var deviceCredentialManager: DeviceCredentialManager private set

    // APIs
    lateinit var publicApi: PublicApi private set
    lateinit var remoteConfigApi: RemoteConfigApi private set
    lateinit var deviceAuthApi: DeviceAuthApi private set

    // Repositories
    lateinit var appRepository: AppRepository private set
    lateinit var homeRepository: HomeRepository private set
    lateinit var authRepository: AuthRepository private set
    lateinit var deviceAuthRepository: DeviceAuthRepository private set
    lateinit var savedSetupRepository: SavedSetupRepository private set
    lateinit var settingsRepository: SettingsRepository private set
    lateinit var wallpaperRepository: WallpaperRepository private set
    lateinit var iconPackRepository: IconPackRepository private set
    lateinit var themeRepository: ThemeRepository private set

    // Use Cases
    lateinit var getInstalledAppsUseCase: GetInstalledAppsUseCase private set
    lateinit var launchAppUseCase: LaunchAppUseCase private set
    lateinit var manageHomeItemsUseCase: ManageHomeItemsUseCase private set
    lateinit var authUseCase: AuthUseCase private set
    lateinit var deviceAuthUseCase: DeviceAuthUseCase private set
    lateinit var manageSavedSetupsUseCase: ManageSavedSetupsUseCase private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Sound & haptic feedback (mirrors js/core/audio-engine.js on web)
        SoundEngine.init(this)

        // Core singletons
        database = AppDatabase.getDatabase(this)
        packageManagerHelper = PackageManagerHelper(this)
        launcherPreferences = LauncherPreferences(this)
        authPreferences = AuthPreferences(this)
        deviceCredentialManager = DeviceCredentialManager(this)

        // Initialize real-time LauncherApps app change listener
        appChangeCallbackManager = AppChangeCallbackManager(this)
        appChangeCallbackManager.startListening()

        // Initialize Android AppWidgetHost for hosting system widgets
        appWidgetHost = com.aura.launcher.launcher.widget.AuraAppWidgetHost(this)
        // startListening() is lifecycle-bound to MainActivity (onStart/onStop) to prevent DeadObjectException

        // Network Layer Retrofit & APIs (Points to live admin backend: aura-launcher.unaux.com/api/v1)
        val retrofit = ApiClient.createRetrofit(authPreferences)
        val wallpaperApi = retrofit.create(WallpaperApi::class.java)
        val iconPackApi = retrofit.create(IconPackApi::class.java)
        val themeApi = retrofit.create(ThemeApi::class.java)
        publicApi = retrofit.create(PublicApi::class.java)
        remoteConfigApi = retrofit.create(RemoteConfigApi::class.java)
        val authApi = retrofit.create(AuthApi::class.java)
        deviceAuthApi = retrofit.create(DeviceAuthApi::class.java)

        // Repositories
        appRepository = AppRepositoryImpl(database.appDao(), packageManagerHelper)
        homeRepository = HomeRepositoryImpl(
            database.homeItemDao(),
            launcherPreferences,
            database.folderDao(),
            database.appDao()
        )
        authRepository = AuthRepositoryImpl(
            database.userDao(),
            authPreferences,
            FirebaseAuth.getInstance(),
            authApi
        )
        deviceAuthRepository = DeviceAuthRepositoryImpl(
            deviceCredentialManager,
            database.trustedDeviceDao(),
            deviceAuthApi,
            FirebaseAuth.getInstance()
        )
        savedSetupRepository = SavedSetupRepositoryImpl(
            database.savedSetupDao(),
            database.favoriteDao(),
            database.homeItemDao()
        )
        settingsRepository = SettingsRepositoryImpl(launcherPreferences, packageManagerHelper)
        wallpaperRepository = WallpaperRepositoryImpl(this, wallpaperApi, database.cachedContentDao())
        iconPackRepository = IconPackRepositoryImpl(iconPackApi, database.cachedContentDao())
        themeRepository = ThemeRepositoryImpl(themeApi, database.cachedContentDao())

        // Use Cases
        getInstalledAppsUseCase = GetInstalledAppsUseCase(appRepository)
        launchAppUseCase = LaunchAppUseCase(appRepository)
        manageHomeItemsUseCase = ManageHomeItemsUseCase(homeRepository, appRepository)
        authUseCase = AuthUseCase(authRepository)
        deviceAuthUseCase = DeviceAuthUseCase(deviceAuthRepository)
        manageSavedSetupsUseCase = ManageSavedSetupsUseCase(savedSetupRepository, homeRepository)
    }

    companion object {
        lateinit var instance: AuraLauncherApp private set
    }
}
