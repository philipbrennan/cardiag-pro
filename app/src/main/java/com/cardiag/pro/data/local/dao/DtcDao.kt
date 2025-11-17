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

    /**
     * Get DTC by code with optional manufacturer filtering.
     * Returns generic code or manufacturer-specific code.
     */
    @Query("SELECT * FROM dtc_codes WHERE code = :code AND (manufacturer IS NULL OR manufacturer = :manufacturer) ORDER BY manufacturer DESC LIMIT 1")
    suspend fun getDtcByCodeWithManufacturer(code: String, manufacturer: String?): DtcEntity?

    /**
     * Get all codes for a manufacturer (both generic and manufacturer-specific).
     */
    @Query("SELECT * FROM dtc_codes WHERE manufacturer IS NULL OR manufacturer = :manufacturer ORDER BY code")
    suspend fun getAllCodesForManufacturer(manufacturer: String): List<DtcEntity>

    /**
     * Get code count by manufacturer.
     */
    @Query("SELECT COUNT(*) FROM dtc_codes WHERE manufacturer = :manufacturer")
    suspend fun getManufacturerCodeCount(manufacturer: String): Int

    /**
     * Get generic code count.
     */
    @Query("SELECT COUNT(*) FROM dtc_codes WHERE manufacturer IS NULL")
    suspend fun getGenericCodeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(codes: List<DtcEntity>)

    @Query("SELECT COUNT(*) FROM dtc_codes")
    suspend fun getCodeCount(): Int

    @Query("DELETE FROM dtc_codes")
    suspend fun deleteAll()
}
