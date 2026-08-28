package com.aura.launcher

import android.app.Application
import com.aura.launcher.core.database.AppDatabase
import com.aura.launcher.core.datastore.AuthPreferences
import com.aura.launcher.core.datastore.LauncherPreferences
import com.aura.launcher.core.network.ApiClient
import com.aura.launcher.core.utils.PackageManagerHelper
import com.aura.launcher.data.remote.api.*
import com.aura.launcher.data.repository.*
import com.aura.launcher.domain.repository.*
import com.aura.launcher.domain.usecase.*

class AuraLauncherApp : Application() {

    lateinit var database: AppDatabase private set
    lateinit var packageManagerHelper: PackageManagerHelper private set
    lateinit var launcherPreferences: LauncherPreferences private set
    lateinit var authPreferences: AuthPreferences private set

    // Repositories
    lateinit var appRepository: AppRepository private set
    lateinit var homeRepository: HomeRepository private set
    lateinit var authRepository: AuthRepository private set
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
    lateinit var manageSavedSetupsUseCase: ManageSavedSetupsUseCase private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Core singletons
        database = AppDatabase.getDatabase(this)
        packageManagerHelper = PackageManagerHelper(this)
        launcherPreferences = LauncherPreferences(this)
        authPreferences = AuthPreferences(this)

        // Network Layer Retrofit & APIs
        val retrofit = ApiClient.createRetrofit(authPreferences)
        val wallpaperApi = retrofit.create(WallpaperApi::class.java)
        val iconPackApi = retrofit.create(IconPackApi::class.java)
        val themeApi = retrofit.create(ThemeApi::class.java)

        // Repositories
        appRepository = AppRepositoryImpl(database.appDao(), packageManagerHelper)
        homeRepository = HomeRepositoryImpl(database.homeItemDao(), launcherPreferences)
        authRepository = AuthRepositoryImpl(database.userDao(), authPreferences)
        savedSetupRepository = SavedSetupRepositoryImpl(
            database.savedSetupDao(),
            database.favoriteDao(),
            database.homeItemDao()
        )
        settingsRepository = SettingsRepositoryImpl(launcherPreferences, packageManagerHelper)
        wallpaperRepository = WallpaperRepositoryImpl(this, wallpaperApi)
        iconPackRepository = IconPackRepositoryImpl(iconPackApi)
        themeRepository = ThemeRepositoryImpl(themeApi)

        // Use Cases
        getInstalledAppsUseCase = GetInstalledAppsUseCase(appRepository)
        launchAppUseCase = LaunchAppUseCase(appRepository)
        manageHomeItemsUseCase = ManageHomeItemsUseCase(homeRepository, appRepository)
        authUseCase = AuthUseCase(authRepository)
        manageSavedSetupsUseCase = ManageSavedSetupsUseCase(savedSetupRepository, homeRepository)
    }

    companion object {
        lateinit var instance: AuraLauncherApp private set
    }
}
