package com.cardiag.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cardiag.pro.data.local.entity.DtcEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for DTC codes.
 */
@Dao
interface DtcDao {

    @Query("SELECT * FROM dtc_codes WHERE code = :code LIMIT 1")
    suspend fun getDtcByCode(code: String): DtcEntity?

    @Query("SELECT * FROM dtc_codes WHERE manufacturer IS NULL ORDER BY code")
    suspend fun getAllGenericCodes(): List<DtcEntity>

    @Query("SELECT * FROM dtc_codes WHERE manufacturer = :manufacturer ORDER BY code")
    suspend fun getManufacturerCodes(manufacturer: String): List<DtcEntity>

    @Query("SELECT * FROM dtc_codes WHERE system = :system ORDER BY code")
    suspend fun getCodesBySystem(system: String): List<DtcEntity>

    @Query("SELECT * FROM dtc_codes WHERE code LIKE :pattern")
    suspend fun searchCodes(pattern: String): List<DtcEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(codes: List<DtcEntity>)

    @Query("SELECT COUNT(*) FROM dtc_codes")
    suspend fun getCodeCount(): Int

    @Query("DELETE FROM dtc_codes")
    suspend fun deleteAll()
}
