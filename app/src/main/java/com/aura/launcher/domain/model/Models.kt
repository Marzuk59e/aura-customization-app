package com.aura.launcher.domain.model

data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val category: String = "General",
    val isHidden: Boolean = false,
    val installTime: Long = 0L,
    val launchCount: Int = 0
) {
    val componentKey: String
        get() = "$packageName/$activityName"
}

enum class HomeItemType {
    APP,
    SHORTCUT,
    FOLDER,
    WIDGET
}

data class HomeItem(
    val id: Long = 0,
    val pageIndex: Int, // 0 = First Page, -1 = Dock
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val itemType: HomeItemType = HomeItemType.APP,
    val packageName: String? = null,
    val activityName: String? = null,
    val label: String? = null,
    val iconUri: String? = null,
    val folderId: Long? = null,
    val widgetId: Int? = null,
    val widgetProvider: String? = null
)

data class Folder(
    val id: Long = 0,
    val title: String,
    val color: String = "#7000FF",
    val apps: List<AppInfo> = emptyList()
)

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isGuest: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class SavedSetup(
    val id: Long = 0,
    val userId: String,
    val title: String,
    val description: String = "",
    val previewWallpaper: String = "",
    val iconPackName: String = "Default Modern",
    val gridRows: Int = 5,
    val gridCols: Int = 4,
    val layoutJson: String = "", // serialized HomeItem list
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

enum class FavoriteType {
    THEME,
    WALLPAPER,
    ICON_PACK,
    SETUP
}

data class FavoriteItem(
    val id: Long = 0,
    val userId: String,
    val type: FavoriteType,
    val targetId: String,
    val title: String,
    val previewUrl: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

data class LauncherSettings(
    val gridRows: Int = 5,
    val gridCols: Int = 4,
    val dockMaxItems: Int = 5,
    val iconScale: Float = 1.0f,
    val showLabels: Boolean = true,
    val darkTheme: Boolean = true,
    val isDefaultLauncher: Boolean = false
)
