package com.aura.launcher.customization.explore

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.launcher.core.network.NetworkResult
import com.aura.launcher.customization.vibesync.ExtractedVibePalette
import com.aura.launcher.customization.vibesync.VibeSyncEngine
import com.aura.launcher.domain.model.*
import com.aura.launcher.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ExploreTab {
    WALLPAPERS,
    ICON_PACKS,
    THEMES
}

class ExploreViewModel(
    private val wallpaperRepository: WallpaperRepository,
    private val iconPackRepository: IconPackRepository,
    private val themeRepository: ThemeRepository,
    private val savedSetupRepository: SavedSetupRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ExploreTab.WALLPAPERS)
    val selectedTab: StateFlow<ExploreTab> = _selectedTab.asStateFlow()

    private val _wallpapers = MutableStateFlow<List<RemoteWallpaper>>(emptyList())
    val wallpapers: StateFlow<List<RemoteWallpaper>> = _wallpapers.asStateFlow()

    private val _iconPacks = MutableStateFlow<List<RemoteIconPack>>(emptyList())
    val iconPacks: StateFlow<List<RemoteIconPack>> = _iconPacks.asStateFlow()

    private val _themes = MutableStateFlow<List<RemoteTheme>>(emptyList())
    val themes: StateFlow<List<RemoteTheme>> = _themes.asStateFlow()

    private val _extractedPalette = MutableStateFlow<ExtractedVibePalette?>(null)
    val extractedPalette: StateFlow<ExtractedVibePalette?> = _extractedPalette.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tab: ExploreTab) {
        _selectedTab.value = tab
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val wpResult = wallpaperRepository.getWallpapers()) {
                is NetworkResult.Success -> _wallpapers.value = wpResult.data
                else -> {}
            }
            when (val ipResult = iconPackRepository.getIconPacks()) {
                is NetworkResult.Success -> _iconPacks.value = ipResult.data
                else -> {}
            }
            when (val themeResult = themeRepository.getThemes()) {
                is NetworkResult.Success -> _themes.value = themeResult.data
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun applyWallpaper(wallpaper: RemoteWallpaper, onApplied: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = wallpaperRepository.applyWallpaper(wallpaper.imageUrl)
            onApplied(success)
        }
    }

    fun favoriteItem(userId: String, type: FavoriteType, id: String, title: String, previewUrl: String) {
        viewModelScope.launch {
            savedSetupRepository.toggleFavorite(userId, type, id, title, previewUrl)
        }
    }

    fun processVibeSync(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.value = true
            val palette = withContext(Dispatchers.Default) {
                VibeSyncEngine.extractPalette(bitmap)
            }
            _extractedPalette.value = palette
            _isLoading.value = false
        }
    }

    fun applyVibeSync(onApplied: (Boolean) -> Unit) {
        viewModelScope.launch {
            val palette = _extractedPalette.value ?: return@launch
            // In a real app, this would update a global theme or preference
            // For now, we simulate success
            onApplied(true)
        }
    }
}
