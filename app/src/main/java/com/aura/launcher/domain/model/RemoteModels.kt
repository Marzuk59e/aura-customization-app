package com.aura.launcher.domain.model

data class RemoteWallpaper(
    val id: String,
    val title: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val category: String = "Abstract",
    val tier: String = "free",
    val downloads: Int = 0
)

data class RemoteIconPack(
    val id: String,
    val name: String,
    val description: String,
    val iconCount: Int,
    val previewUrl: String,
    val tier: String = "free",
    val author: String = "Aura Studio"
)

data class RemoteTheme(
    val id: String,
    val name: String,
    val description: String,
    val primaryColor: String,
    val secondaryColor: String,
    val previewUrl: String,
    val tier: String = "free"
)

data class RemoteWidget(
    val id: String,
    val name: String,
    val kind: String,
    val previewUrl: String
)
