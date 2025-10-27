package com.[domain].[app].data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database entity for [Entity].
 *
 * Represents a [entity] stored in the local Room database.
 *
 * @property id Unique identifier
 * @property name Name of the [entity]
 * @property description Description text
 * @property timestamp Creation/update timestamp in milliseconds
 * @property isFavorite Whether this [entity] is marked as favorite
 */
@Entity(
    tableName = "[table_name]",
    indices = [
        Index(value = ["name"]),
        Index(value = ["timestamp"]),
        Index(value = ["is_favorite"])
    ]
)
data class [Entity]Entity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)

/**
 * Domain model for [Entity].
 *
 * This is the model used throughout the app (outside the data layer).
 * Separating domain models from database entities allows flexibility.
 */
data class [Entity](
    val id: String,
    val name: String,
    val description: String? = null,
    val timestamp: Long,
    val isFavorite: Boolean = false
) {
    /**
     * Check if this [entity] is new (created within last hour)
     */
    fun isNew(): Boolean {
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        return timestamp > oneHourAgo
    }

    /**
     * Get formatted timestamp
     */
    fun getFormattedDate(): String {
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(timestamp))
    }
}

/**
 * UI state wrapper for [Entity]
 *
 * Use this for UI-specific state (like selection, loading, etc.)
 */
data class [Entity]UiState(
    val entity: [Entity],
    val isSelected: Boolean = false,
    val isLoading: Boolean = false
)
