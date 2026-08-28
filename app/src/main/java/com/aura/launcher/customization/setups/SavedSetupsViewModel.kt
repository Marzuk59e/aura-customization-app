package com.aura.launcher.customization.setups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.launcher.domain.model.FavoriteItem
import com.aura.launcher.domain.model.FavoriteType
import com.aura.launcher.domain.model.HomeItem
import com.aura.launcher.domain.model.SavedSetup
import com.aura.launcher.domain.usecase.AuthUseCase
import com.aura.launcher.domain.usecase.ManageHomeItemsUseCase
import com.aura.launcher.domain.usecase.ManageSavedSetupsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SavedSetupsViewModel(
    private val manageSavedSetupsUseCase: ManageSavedSetupsUseCase,
    private val manageHomeItemsUseCase: ManageHomeItemsUseCase,
    private val authUseCase: AuthUseCase
) : ViewModel() {

    private val currentUserId = authUseCase.currentUser.map { it?.id ?: "guest_default" }

    val savedSetups: StateFlow<List<SavedSetup>> = currentUserId.flatMapLatest { uid ->
        manageSavedSetupsUseCase.getUserSetups(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteItem>> = currentUserId.flatMapLatest { uid ->
        manageSavedSetupsUseCase.getUserFavorites(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveActiveScreenAsSetup(title: String, description: String = "") {
        viewModelScope.launch {
            val user = authUseCase.getCurrentUser()
            val userId = user?.id ?: "guest_default"
            val items = manageHomeItemsUseCase.getAllItems().first()

            manageSavedSetupsUseCase.saveCurrentSetup(
                userId = userId,
                title = if (title.isBlank()) "My Custom Setup" else title.trim(),
                description = description,
                previewWallpaper = "",
                iconPackName = "Default Modern",
                gridRows = 5,
                gridCols = 4,
                items = items
            )
        }
    }

    fun applySetup(setupId: Long, onApplied: () -> Unit = {}) {
        viewModelScope.launch {
            val success = manageSavedSetupsUseCase.applySetup(setupId)
            if (success) onApplied()
        }
    }

    fun deleteSetup(setup: SavedSetup) {
        viewModelScope.launch {
            manageSavedSetupsUseCase.deleteSetup(setup)
        }
    }

    fun toggleFavorite(type: FavoriteType, targetId: String, title: String, previewUrl: String = "") {
        viewModelScope.launch {
            val user = authUseCase.getCurrentUser()
            val userId = user?.id ?: "guest_default"
            manageSavedSetupsUseCase.toggleFavorite(userId, type, targetId, title, previewUrl)
        }
    }
}
