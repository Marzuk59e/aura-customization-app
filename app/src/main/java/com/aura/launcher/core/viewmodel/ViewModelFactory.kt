package com.aura.launcher.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aura.launcher.AuraLauncherApp
import com.aura.launcher.auth.AuthViewModel
import com.aura.launcher.customization.explore.ExploreViewModel
import com.aura.launcher.customization.setups.SavedSetupsViewModel
import com.aura.launcher.launcher.appdrawer.AppDrawerViewModel
import com.aura.launcher.launcher.home.HomeViewModel
import com.aura.launcher.launcher.settings.SettingsViewModel

class ViewModelFactory(private val app: AuraLauncherApp) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    app.manageHomeItemsUseCase,
                    app.getInstalledAppsUseCase,
                    app.launchAppUseCase,
                    app.appWidgetHostHelper
                ) as T
            }
            modelClass.isAssignableFrom(AppDrawerViewModel::class.java) -> {
                AppDrawerViewModel(
                    app.getInstalledAppsUseCase,
                    app.launchAppUseCase,
                    app.packageManagerHelper
                ) as T
            }
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(app.authUseCase) as T
            }
            modelClass.isAssignableFrom(SavedSetupsViewModel::class.java) -> {
                SavedSetupsViewModel(
                    app.manageSavedSetupsUseCase,
                    app.manageHomeItemsUseCase,
                    app.authUseCase
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(app.settingsRepository) as T
            }
            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> {
                ExploreViewModel(
                    app.wallpaperRepository,
                    app.iconPackRepository,
                    app.themeRepository,
                    app.savedSetupRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
