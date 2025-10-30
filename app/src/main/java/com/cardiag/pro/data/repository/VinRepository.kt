package com.cardiag.pro.data.repository

import com.cardiag.pro.data.connection.ELM327Protocol
import com.cardiag.pro.data.model.Manufacturer
import com.cardiag.pro.data.model.Result
import com.cardiag.pro.data.model.VehicleInfo
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for VIN decoding operations.
 */
@Singleton
class VinRepository @Inject constructor(
    private val elm327Protocol: ELM327Protocol
) {

    /**
     * Read VIN from vehicle using OBD2.
     * Mode 09, PID 02 - VIN (17 characters)
     */
    suspend fun readVin(): Result<VehicleInfo> {
        try {
            Timber.d("Reading VIN from vehicle")

            // Send VIN request: Mode 09, PID 02
            val response = elm327Protocol.sendCommand("0902")

            if (response == null) {
                return Result.Error(Exception("Failed to read VIN from vehicle"))
            }

            // Parse VIN from response
            // Response format: 49 02 01 XX XX XX XX ... (multi-line response)
            val vin = parseVin(response)

            if (vin == null || !isValidVin(vin)) {
                return Result.Error(Exception("Invalid VIN received: $response"))
            }

            val manufacturer = Manufacturer.fromVin(vin)
            Timber.i("VIN decoded successfully: $vin, Manufacturer: $manufacturer")

            return Result.Success(
                VehicleInfo(
                    vin = vin,
                    manufacturer = manufacturer,
                    year = extractYearFromVin(vin),
                    model = null, // We don't decode model from VIN yet
                    isManuallyEntered = false
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to read VIN")
            return Result.Error(e)
        }
    }

    /**
     * Create vehicle info with manual manufacturer selection.
     */
    fun createManualVehicleInfo(
        vin: String?,
        manufacturer: Manufacturer,
        year: Int? = null,
        model: String? = null
    ): VehicleInfo {
        return VehicleInfo(
            vin = vin ?: "MANUAL",
            manufacturer = manufacturer,
            year = year,
            model = model,
            isManuallyEntered = true
        )
    }

    /**
     * Parse VIN from OBD2 response.
     * Multi-line response format:
     * 49 02 01 XX XX XX XX XX
     * 49 02 02 XX XX XX XX XX
     * 49 02 03 XX XX XX XX
     */
    private fun parseVin(response: String): String? {
        try {
            // Remove spaces and split into bytes
            val bytes = response.replace(" ", "")
                .chunked(2)
                .mapNotNull { it.toIntOrNull(16) }

            // Find start of VIN data (after 49 02 01)
            val vinBytes = mutableListOf<Int>()
            var foundStart = false

            for (i in bytes.indices) {
                if (i >= 2 && bytes[i - 2] == 0x49 && bytes[i - 1] == 0x02) {
                    foundStart = true
                    continue
                }
                if (foundStart && bytes[i] in 32..126) { // Printable ASCII
                    vinBytes.add(bytes[i])
                    if (vinBytes.size == 17) break
                }
            }

            if (vinBytes.size == 17) {
                return vinBytes.map { it.toChar() }.joinToString("")
            }

            return null
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse VIN from response")
            return null
        }
    }

    /**
     * Validate VIN format.
     */
    private fun isValidVin(vin: String): Boolean {
        if (vin.length != 17) return false

        // VIN should not contain I, O, or Q
        if (vin.contains('I') || vin.contains('O') || vin.contains('Q')) {
            return false
        }

        // Should be alphanumeric
        return vin.all { it.isLetterOrDigit() }
    }

    /**
     * Extract manufacturing year from VIN (10th character).
     */
    private fun extractYearFromVin(vin: String): Int? {
        if (vin.length < 10) return null

        val yearChar = vin[9]

        // Year encoding: 2001-2009 = 1-9, 2010-2030 = A-Y (excluding I, O, Q)
        return when (yearChar) {
            in '1'..'9' -> 2000 + yearChar.digitToInt()
            in 'A'..'H' -> 2010 + (yearChar - 'A')
            in 'J'..'N' -> 2018 + (yearChar - 'J')
            in 'P'..'P' -> 2023
            in 'R'..'Y' -> 2024 + (yearChar - 'R')
            else -> null
        }
    }
}
