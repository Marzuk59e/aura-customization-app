package com.aura.launcher.launcher.appdrawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.launcher.domain.model.AppInfo
import com.aura.launcher.domain.usecase.GetInstalledAppsUseCase
import com.aura.launcher.domain.usecase.LaunchAppUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppDrawerViewModel(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val launchAppUseCase: LaunchAppUseCase
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

    init {
        viewModelScope.launch {
            getInstalledAppsUseCase.refresh()
        }
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
