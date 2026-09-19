package com.aura.launcher.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        CachedThemeEntity::class
    ],
    version = 4,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v3 -> v4 (Phase 4.6): the device-auth feature is gone, so its
        // trusted_devices table is dropped. An explicit migration keeps every
        // other table (home layout, saved setups, favorites) intact on update.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS trusted_devices")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aura_launcher.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
