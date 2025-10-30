package com.cardiag.pro.data.local

import android.content.Context
import com.cardiag.pro.data.local.dao.DtcDao
import com.cardiag.pro.data.local.entity.DtcEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializes the database with DTC codes from CSV files.
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val dtcDao: DtcDao
) {

    /**
     * Load DTC codes from assets if database is empty.
     */
    suspend fun initializeIfNeeded(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val count = dtcDao.getCodeCount()
                if (count == 0) {
                    Timber.i("Database empty, loading DTC codes from assets")
                    loadDtcCodes(context)
                } else {
                    Timber.d("Database already initialized with $count codes")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize database")
            }
        }
    }

    /**
     * Load DTC codes from CSV file in assets.
     */
    private suspend fun loadDtcCodes(context: Context) {
        try {
            val codes = mutableListOf<DtcEntity>()

            // Load generic codes
            context.assets.open("dtc_codes_generic.csv").use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                // Skip header
                reader.readLine()

                reader.forEachLine { line ->
                    if (line.isNotBlank()) {
                        val parts = line.split(",")
                        if (parts.size >= 4) {
                            codes.add(
                                DtcEntity(
                                    code = parts[0].trim(),
                                    description = parts[1].trim(),
                                    system = parts[2].trim(),
                                    severity = parts[3].trim(),
                                    manufacturer = null // Generic code
                                )
                            )
                        }
                    }
                }
            }

            dtcDao.insertAll(codes)
            Timber.i("Loaded ${codes.size} DTC codes into database")

        } catch (e: Exception) {
            Timber.e(e, "Failed to load DTC codes from assets")
        }
    }

    /**
     * Clear all DTC codes from database.
     */
    suspend fun clearDatabase() {
        withContext(Dispatchers.IO) {
            try {
                dtcDao.deleteAll()
                Timber.i("Database cleared")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear database")
            }
        }
    }
}
