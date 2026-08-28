package com.aura.launcher.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aura.launcher.domain.model.FavoriteType
import com.aura.launcher.domain.model.HomeItemType

@Entity(tableName = "installed_apps")
data class AppEntity(
    @PrimaryKey val componentKey: String, // packageName/activityName
    val packageName: String,
    val activityName: String,
    val label: String,
    val category: String = "General",
    val isHidden: Boolean = false,
    val installTime: Long = 0L,
    val launchCount: Int = 0
)

@Entity(tableName = "home_items")
data class HomeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageIndex: Int, // 0..N, -1 for Dock
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val itemType: String = "APP", // APP, SHORTCUT, FOLDER, WIDGET
    val packageName: String? = null,
    val activityName: String? = null,
    val label: String? = null,
    val iconUri: String? = null,
    val folderId: Long? = null,
    val widgetId: Int? = null,
    val widgetProvider: String? = null
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val color: String = "#7000FF",
    val appKeysJson: String = "[]" // JSON array of component keys
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isGuest: Boolean = false,
    val isCurrentLoggedIn: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_setups")
data class SavedSetupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val description: String = "",
    val previewWallpaper: String = "",
    val iconPackName: String = "Default Modern",
    val gridRows: Int = 5,
    val gridCols: Int = 4,
    val layoutJson: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val type: String, // THEME, WALLPAPER, ICON_PACK, SETUP
    val targetId: String,
    val title: String,
    val previewUrl: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
