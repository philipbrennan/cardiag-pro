package com.cardiag.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cardiag.pro.data.local.entity.DiagnosticSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for diagnostic sessions.
 */
@Dao
interface DiagnosticSessionDao {

    @Query("SELECT * FROM diagnostic_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<DiagnosticSessionEntity>>

    @Query("SELECT * FROM diagnostic_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): DiagnosticSessionEntity?

    @Query("SELECT * FROM diagnostic_sessions WHERE vin = :vin ORDER BY timestamp DESC")
    fun getSessionsByVin(vin: String): Flow<List<DiagnosticSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: DiagnosticSessionEntity): Long

    @Query("DELETE FROM diagnostic_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM diagnostic_sessions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM diagnostic_sessions")
    suspend fun getSessionCount(): Int
}
