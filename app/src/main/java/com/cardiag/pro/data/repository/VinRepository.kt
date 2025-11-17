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
     * Will retry up to 3 times on failure.
     */
    suspend fun readVin(): Result<VehicleInfo> {
        val maxAttempts = 3
        var lastError: Exception? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                Timber.d("=== VIN read attempt ${attempt + 1}/$maxAttempts ===")

                // Send VIN request: Mode 09, PID 02
                Timber.d("Sending VIN request command: 0902")
                val response = elm327Protocol.sendCommand("0902")

            Timber.d("Raw VIN response received: '$response'")
            Timber.d("Response length: ${response?.length ?: 0} characters")

            if (response == null) {
                Timber.e("VIN response is null")
                return Result.Error(Exception("Failed to read VIN from vehicle - no response"))
            }

            if (response.contains("NO DATA", ignoreCase = true)) {
                Timber.e("Vehicle does not support VIN reading (Mode 09 PID 02)")
                return Result.Error(Exception("VIN not supported by vehicle"))
            }

            if (response.contains("ERROR", ignoreCase = true)) {
                Timber.e("ELM327 returned error: $response")
                return Result.Error(Exception("ELM327 error reading VIN: $response"))
            }

            // Parse VIN from response
            // Response format: 49 02 01 XX XX XX XX ... (multi-line response)
            Timber.d("Parsing VIN from response...")
            val vin = parseVin(response)

            if (vin == null) {
                Timber.e("Failed to parse VIN from response: $response")
                return Result.Error(Exception("Could not parse VIN from response"))
            }

            Timber.d("Parsed VIN: '$vin' (${vin.length} characters)")

            if (!isValidVin(vin)) {
                Timber.e("VIN validation failed: '$vin' - invalid format")
                return Result.Error(Exception("Invalid VIN format: $vin"))
            }

            val manufacturer = Manufacturer.fromVin(vin)
            val year = extractYearFromVin(vin)
            
            Timber.i("=== VIN decoded successfully ===")
            Timber.i("VIN: $vin")
            Timber.i("Manufacturer: ${manufacturer.displayName}")
            Timber.i("Year: ${year ?: "Unknown"}")

            return Result.Success(
                VehicleInfo(
                    vin = vin,
                    manufacturer = manufacturer,
                    year = year,
                    model = null, // We don't decode model from VIN yet
                    isManuallyEntered = false
                )
            )
            } catch (e: Exception) {
                Timber.w(e, "VIN read attempt ${attempt + 1} failed")
                lastError = e
                
                // Don't retry if it's a "not supported" error
                if (e.message?.contains("not supported", ignoreCase = true) == true) {
                    Timber.e("Vehicle does not support VIN reading, aborting retries")
                    return Result.Error(e)
                }
                
                // Wait a bit before retry (except on last attempt)
                if (attempt < maxAttempts - 1) {
                    Timber.d("Waiting 500ms before retry...")
                    kotlinx.coroutines.delay(500)
                }
            }
        }
        
        // All attempts failed
        Timber.e("All $maxAttempts VIN read attempts failed")
        return Result.Error(lastError ?: Exception("Failed to read VIN after $maxAttempts attempts"))
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
     * 
     * Expected formats:
     * 1. Multi-line: "49 02 01 XX XX XX\n49 02 02 XX XX XX\n49 02 03 XX XX"
     * 2. Single-line: "49 02 01 XX XX XX XX XX XX XX..."
     * 3. CAN frames: "014\n0: 49 02 01 XX XX XX\n1: XX XX XX XX XX XX"
     */
    private fun parseVin(response: String): String? {
        try {
            Timber.d("--- VIN Parsing Details ---")
            Timber.d("Response lines: ${response.split('\n').size}")
            
            // Clean response: remove line numbers, colons, and extra whitespace
            val cleaned = response
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { line ->
                    // Remove CAN frame numbers like "0:", "1:", etc.
                    if (line.matches(Regex("^[0-9A-F]:\\s*.*"))) {
                        line.substring(line.indexOf(':') + 1).trim()
                    } else {
                        line
                    }
                }
                .joinToString(" ")
            
            Timber.d("Cleaned response: '$cleaned'")
            
            // Remove all spaces and convert to bytes
            val hexString = cleaned.replace(Regex("\\s+"), "")
            Timber.d("Hex string (no spaces): $hexString")
            
            if (hexString.length < 10) {
                Timber.e("Hex string too short: ${hexString.length} characters")
                return null
            }
            
            val bytes = hexString
                .chunked(2)
                .mapNotNull { 
                    val byte = it.toIntOrNull(16)
                    if (byte == null) {
                        Timber.w("Failed to parse hex byte: '$it'")
                    }
                    byte
                }

            Timber.d("Parsed ${bytes.size} bytes from response")

            // Method 1: Look for 49 02 sequence followed by frame number
            val vinBytes = mutableListOf<Int>()
            var i = 0
            
            while (i < bytes.size - 2) {
                if (bytes[i] == 0x49 && bytes[i + 1] == 0x02) {
                    Timber.d("Found VIN header at byte $i")
                    // Skip header (49 02) and frame number
                    i += 3
                    
                    // Collect ASCII characters
                    while (i < bytes.size && vinBytes.size < 17) {
                        val byte = bytes[i]
                        if (byte in 32..126) { // Printable ASCII
                            vinBytes.add(byte)
                            i++
                        } else if (byte == 0x49) {
                            // Start of next frame
                            break
                        } else {
                            i++
                        }
                    }
                } else {
                    i++
                }
            }

            Timber.d("Extracted ${vinBytes.size} VIN characters")
            
            if (vinBytes.size >= 17) {
                val vin = vinBytes.take(17).map { it.toChar() }.joinToString("")
                Timber.d("Parsed VIN (Method 1): '$vin'")
                return vin
            }

            // Method 2: Just extract all printable ASCII characters (fallback)
            Timber.d("Method 1 failed, trying Method 2 (extract all ASCII)")
            val allAscii = bytes.filter { it in 32..126 }.map { it.toChar() }.joinToString("")
            Timber.d("All ASCII characters: '$allAscii' (${allAscii.length} chars)")
            
            if (allAscii.length >= 17) {
                // Find a 17-character substring that looks like a VIN
                for (start in 0..(allAscii.length - 17)) {
                    val candidate = allAscii.substring(start, start + 17)
                    if (candidate.all { it.isLetterOrDigit() } && 
                        !candidate.contains('I') && 
                        !candidate.contains('O') && 
                        !candidate.contains('Q')) {
                        Timber.d("Found VIN candidate (Method 2): '$candidate'")
                        return candidate
                    }
                }
            }

            Timber.e("Failed to extract valid VIN from response")
            return null
        } catch (e: Exception) {
            Timber.e(e, "Exception during VIN parsing")
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
