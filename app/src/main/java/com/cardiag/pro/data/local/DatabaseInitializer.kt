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
            var totalLoaded = 0

            // Load generic codes
            val genericCount = loadCodesFromCsv(context, "dtc_codes_generic.csv", null, codes)
            totalLoaded += genericCount
            Timber.i("Loaded $genericCount generic DTC codes")

            // Load BMW codes
            val bmwCount = loadCodesFromCsv(context, "dtc_codes_bmw.csv", "BMW", codes)
            totalLoaded += bmwCount
            Timber.i("Loaded $bmwCount BMW-specific DTC codes")

            // Load VW/Audi codes
            val vwCount = loadCodesFromCsv(context, "dtc_codes_vw.csv", "VW", codes)
            totalLoaded += vwCount
            Timber.i("Loaded $vwCount VW/Audi-specific DTC codes")

            // Load Nissan codes
            val nissanCount = loadCodesFromCsv(context, "dtc_codes_nissan.csv", "NISSAN", codes)
            totalLoaded += nissanCount
            Timber.i("Loaded $nissanCount Nissan-specific DTC codes")

            // Load ABS codes (generic)
            val absCount = loadCodesFromCsv(context, "dtc_codes_abs.csv", null, codes)
            totalLoaded += absCount
            Timber.i("Loaded $absCount ABS DTC codes")

            // Load SRS/Airbag codes (generic)
            val srsCount = loadCodesFromCsv(context, "dtc_codes_srs.csv", null, codes)
            totalLoaded += srsCount
            Timber.i("Loaded $srsCount SRS/Airbag DTC codes")

            // Insert all codes in batch
            dtcDao.insertAll(codes)
            Timber.i("Successfully loaded $totalLoaded total DTC codes into database")

        } catch (e: Exception) {
            Timber.e(e, "Failed to load DTC codes from assets")
        }
    }

    /**
     * Load codes from a specific CSV file.
     * CSV format: code,system,manufacturer,description,possible_causes,severity
     */
    private fun loadCodesFromCsv(
        context: Context,
        filename: String,
        manufacturer: String?,
        codes: MutableList<DtcEntity>
    ): Int {
        var count = 0
        try {
            context.assets.open(filename).use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                // Skip header
                reader.readLine()

                reader.forEachLine { line ->
                    if (line.isNotBlank()) {
                        try {
                            val parts = parseCsvLine(line)
                            if (parts.size >= 6) {
                                codes.add(
                                    DtcEntity(
                                        code = parts[0].trim(),
                                        system = parts[1].trim(),
                                        manufacturer = parts[2].trim().ifBlank { manufacturer },
                                        description = parts[3].trim(),
                                        possibleCauses = parts[4].trim(),
                                        severity = parts[5].trim()
                                    )
                                )
                                count++
                            } else if (parts.size >= 4) {
                                // Legacy format support
                                codes.add(
                                    DtcEntity(
                                        code = parts[0].trim(),
                                        description = parts[1].trim(),
                                        system = parts[2].trim(),
                                        severity = parts[3].trim(),
                                        manufacturer = manufacturer,
                                        possibleCauses = null
                                    )
                                )
                                count++
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to parse line in $filename: $line")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load $filename (file may not exist)")
        }
        return count
    }

    /**
     * Parse CSV line handling commas within quoted fields.
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
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
