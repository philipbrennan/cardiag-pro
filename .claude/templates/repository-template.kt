package com.[domain].[app].data.repository

import com.[domain].[app].data.local.[Entity]Dao
import com.[domain].[app].data.local.[Entity]Entity
import com.[domain].[app].data.model.[Entity]
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing [Entity] data.
 *
 * Provides a clean API for accessing [entity] data from local database
 * and handles data mapping between entity and domain models.
 */
@Singleton
class [Feature]Repository @Inject constructor(
    private val dao: [Entity]Dao
) {

    /**
     * Get all [entities] as a Flow
     */
    fun getAll(): Flow<List<[Entity]>> {
        return dao.getAll()
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    /**
     * Get [entity] by ID
     */
    suspend fun getById(id: String): [Entity]? {
        return dao.getById(id)?.toDomainModel()
    }

    /**
     * Get favorite [entities]
     */
    fun getFavorites(): Flow<List<[Entity]>> {
        return dao.getFavorites()
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    /**
     * Search [entities] by name
     */
    fun search(query: String): Flow<List<[Entity]>> {
        return dao.search("%$query%")
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    /**
     * Insert new [entity]
     */
    suspend fun insert(item: [Entity]) {
        Timber.d("Inserting [entity]: ${item.id}")
        dao.insert(item.toEntity())
    }

    /**
     * Insert multiple [entities]
     */
    suspend fun insertAll(items: List<[Entity]>) {
        Timber.d("Inserting ${items.size} [entities]")
        dao.insertAll(items.map { it.toEntity() })
    }

    /**
     * Update existing [entity]
     */
    suspend fun update(item: [Entity]) {
        Timber.d("Updating [entity]: ${item.id}")
        dao.update(item.toEntity())
    }

    /**
     * Delete [entity]
     */
    suspend fun delete(item: [Entity]) {
        Timber.d("Deleting [entity]: ${item.id}")
        dao.delete(item.toEntity())
    }

    /**
     * Delete all [entities]
     */
    suspend fun deleteAll() {
        Timber.d("Deleting all [entities]")
        dao.deleteAll()
    }

    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(id: String) {
        val item = dao.getById(id)
        if (item != null) {
            dao.update(item.copy(isFavorite = !item.isFavorite))
            Timber.d("Toggled favorite for [entity]: $id")
        }
    }
}

// Extension functions for mapping between entity and domain models

/**
 * Convert database entity to domain model
 */
private fun [Entity]Entity.toDomainModel(): [Entity] {
    return [Entity](
        id = id,
        name = name,
        description = description,
        timestamp = timestamp,
        isFavorite = isFavorite
    )
}

/**
 * Convert domain model to database entity
 */
private fun [Entity].toEntity(): [Entity]Entity {
    return [Entity]Entity(
        id = id,
        name = name,
        description = description,
        timestamp = timestamp,
        isFavorite = isFavorite
    )
}
