package com.aura.launcher.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aura.launcher.data.local.dao.*
import com.aura.launcher.data.local.entities.*

@Database(
    entities = [
        AppEntity::class,
        HomeItemEntity::class,
        FolderEntity::class,
        UserEntity::class,
        SavedSetupEntity::class,
        FavoriteEntity::class,
        CachedWallpaperEntity::class,
        CachedIconPackEntity::class,
        CachedThemeEntity::class,
        TrustedDeviceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun homeItemDao(): HomeItemDao
    abstract fun folderDao(): FolderDao
    abstract fun userDao(): UserDao
    abstract fun savedSetupDao(): SavedSetupDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun cachedContentDao(): CachedContentDao
    abstract fun trustedDeviceDao(): TrustedDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aura_launcher.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
