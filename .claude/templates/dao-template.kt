package com.[domain].[app].data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [Entity] entities.
 *
 * Provides database operations for [entity] data.
 */
@Dao
interface [Entity]Dao {

    /**
     * Get all [entities] ordered by name
     */
    @Query("SELECT * FROM [table_name] ORDER BY name ASC")
    fun getAll(): Flow<List<[Entity]Entity>>

    /**
     * Get [entity] by ID
     */
    @Query("SELECT * FROM [table_name] WHERE id = :id")
    suspend fun getById(id: String): [Entity]Entity?

    /**
     * Get favorite [entities]
     */
    @Query("SELECT * FROM [table_name] WHERE is_favorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<[Entity]Entity>>

    /**
     * Search [entities] by name
     */
    @Query("SELECT * FROM [table_name] WHERE name LIKE :query ORDER BY name ASC")
    fun search(query: String): Flow<List<[Entity]Entity>>

    /**
     * Get [entities] created after timestamp
     */
    @Query("SELECT * FROM [table_name] WHERE timestamp > :timestamp ORDER BY timestamp DESC")
    fun getAfterTimestamp(timestamp: Long): Flow<List<[Entity]Entity>>

    /**
     * Get count of all [entities]
     */
    @Query("SELECT COUNT(*) FROM [table_name]")
    suspend fun getCount(): Int

    /**
     * Insert new [entity]
     * OnConflictStrategy.REPLACE will update if ID already exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: [Entity]Entity)

    /**
     * Insert multiple [entities]
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<[Entity]Entity>)

    /**
     * Update existing [entity]
     */
    @Update
    suspend fun update(item: [Entity]Entity)

    /**
     * Update multiple [entities]
     */
    @Update
    suspend fun updateAll(items: List<[Entity]Entity>)

    /**
     * Delete [entity]
     */
    @Delete
    suspend fun delete(item: [Entity]Entity)

    /**
     * Delete [entity] by ID
     */
    @Query("DELETE FROM [table_name] WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Delete all [entities]
     */
    @Query("DELETE FROM [table_name]")
    suspend fun deleteAll()

    /**
     * Delete [entities] older than timestamp
     */
    @Query("DELETE FROM [table_name] WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int

    /**
     * Update favorite status
     */
    @Query("UPDATE [table_name] SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    /**
     * Transaction example: Insert and delete old items
     */
    @Transaction
    suspend fun insertAndCleanup(newItem: [Entity]Entity, maxAge: Long) {
        insert(newItem)
        val cutoffTimestamp = System.currentTimeMillis() - maxAge
        deleteOlderThan(cutoffTimestamp)
    }
}
