package com.aura.launcher.data.local.dao

import androidx.room.*
import com.aura.launcher.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM installed_apps WHERE isHidden = 0 ORDER BY label ASC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM installed_apps WHERE componentKey = :key LIMIT 1")
    suspend fun getAppByKey(key: String): AppEntity?

    @Query("SELECT * FROM installed_apps WHERE label LIKE '%' || :query || '%' AND isHidden = 0 ORDER BY label ASC")
    fun searchApps(query: String): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppEntity>)

    @Query("DELETE FROM installed_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM installed_apps")
    suspend fun clearAll()

    @Query("UPDATE installed_apps SET launchCount = launchCount + 1 WHERE componentKey = :key")
    suspend fun incrementLaunchCount(key: String)
}

@Dao
interface HomeItemDao {
    @Query("SELECT * FROM home_items WHERE pageIndex = :pageIndex ORDER BY cellY ASC, cellX ASC")
    fun getItemsForPage(pageIndex: Int): Flow<List<HomeItemEntity>>

    @Query("SELECT * FROM home_items WHERE pageIndex = -1 ORDER BY cellX ASC")
    fun getDockItems(): Flow<List<HomeItemEntity>>

    @Query("SELECT * FROM home_items")
    suspend fun getAllHomeItemsList(): List<HomeItemEntity>

    @Query("SELECT * FROM home_items")
    fun getAllHomeItems(): Flow<List<HomeItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: HomeItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<HomeItemEntity>)

    @Update
    suspend fun updateItem(item: HomeItemEntity)

    @Delete
    suspend fun deleteItem(item: HomeItemEntity)

    @Query("DELETE FROM home_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM home_items")
    suspend fun clearAllHomeItems()
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentLoggedIn = 1 LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentLoggedIn = 1 LIMIT 1")
    suspend fun getCurrentUserDirect(): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)

    @Query("UPDATE users SET isCurrentLoggedIn = 0")
    suspend fun clearCurrentSessions()

    @Query("UPDATE users SET isCurrentLoggedIn = 1 WHERE id = :userId")
    suspend fun setCurrentSession(userId: String)
}

@Dao
interface SavedSetupDao {
    @Query("SELECT * FROM saved_setups WHERE userId = :userId ORDER BY createdAt DESC")
    fun getSetupsForUser(userId: String): Flow<List<SavedSetupEntity>>

    @Query("SELECT * FROM saved_setups WHERE id = :id")
    suspend fun getSetupById(id: Long): SavedSetupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetup(setup: SavedSetupEntity): Long

    @Delete
    suspend fun deleteSetup(setup: SavedSetupEntity)

    @Query("UPDATE saved_setups SET isFavorite = :isFav WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFav: Boolean)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY addedAt DESC")
    fun getFavoritesForUser(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND targetId = :targetId AND type = :type)")
    suspend fun isFavorited(userId: String, targetId: String, type: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(fav: FavoriteEntity): Long

    @Query("DELETE FROM favorites WHERE userId = :userId AND targetId = :targetId AND type = :type")
    suspend fun removeFavorite(userId: String, targetId: String, type: String)
}
