package com.aura.launcher.launcher.appdrawer

import android.content.pm.LauncherApps
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.launcher.core.utils.PackageManagerHelper
import com.aura.launcher.domain.model.AppInfo
import com.aura.launcher.domain.usecase.GetInstalledAppsUseCase
import com.aura.launcher.domain.usecase.LaunchAppUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppDrawerViewModel(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val launchAppUseCase: LaunchAppUseCase,
    private val packageManagerHelper: PackageManagerHelper
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val apps: StateFlow<List<AppInfo>> = _searchQuery
        .debounce(150)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                getInstalledAppsUseCase()
            } else {
                getInstalledAppsUseCase.search(query.trim())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var appChangeCallback: LauncherApps.Callback? = null

    init {
        viewModelScope.launch {
            getInstalledAppsUseCase.refresh()
        }
        appChangeCallback = packageManagerHelper.registerAppChangeCallback { packageName, changeType ->
            viewModelScope.launch {
                when (changeType) {
                    com.aura.launcher.core.utils.ChangeType.ADDED, com.aura.launcher.core.utils.ChangeType.CHANGED -> {
                        getInstalledAppsUseCase.refresh()
                    }
                    com.aura.launcher.core.utils.ChangeType.REMOVED -> {
                        getInstalledAppsUseCase.refresh()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        appChangeCallback?.let {
            packageManagerHelper.unregisterAppChangeCallback(it)
        }
        super.onCleared()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun launchApp(app: AppInfo) {
        viewModelScope.launch {
            launchAppUseCase(app.packageName, app.activityName)
        }
    }
}
