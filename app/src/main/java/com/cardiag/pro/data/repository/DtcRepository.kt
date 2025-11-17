package com.cardiag.pro.data.repository

import com.cardiag.pro.data.connection.ELM327Protocol
import com.cardiag.pro.data.local.dao.DiagnosticSessionDao
import com.cardiag.pro.data.local.dao.DtcDao
import com.cardiag.pro.data.local.entity.DiagnosticSessionEntity
import com.cardiag.pro.data.local.entity.DtcEntity
import com.cardiag.pro.data.model.DiagnosticTroubleCode
import com.cardiag.pro.data.model.ECUSystem
import com.cardiag.pro.data.model.FreezeFrameData
import com.cardiag.pro.data.model.Manufacturer
import com.cardiag.pro.data.model.Result
import com.cardiag.pro.data.model.Severity
import com.cardiag.pro.data.parser.FreezeFrameParser
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for DTC operations.
 */
@Singleton
class DtcRepository @Inject constructor(
    private val elm327Protocol: ELM327Protocol,
    private val dtcDao: DtcDao,
    private val sessionDao: DiagnosticSessionDao
) {

    /**
     * Read DTCs from a specific ECU system.
     * Mode 03 - Request emission-related DTCs
     */
    suspend fun readDtcCodes(
        system: ECUSystem = ECUSystem.ENGINE,
        manufacturer: Manufacturer? = null
    ): Result<List<DiagnosticTroubleCode>> {
        try {
            Timber.d("Reading DTCs from ${system.displayName} (Manufacturer: ${manufacturer?.displayName ?: "Unknown"})")

            // Send DTC request: Mode 03 for engine codes
            val response = elm327Protocol.sendCommand("03")

            if (response == null) {
                return Result.Error(Exception("Failed to read DTCs from vehicle"))
            }

            // Parse DTCs from response
            val codes = parseDtcCodes(response, system)

            if (codes.isEmpty()) {
                Timber.i("No DTCs found")
                return Result.Success(emptyList())
            }

            // Enrich codes with descriptions from database (with manufacturer awareness)
            val enrichedCodes = enrichCodesWithDescriptions(codes, manufacturer)

            Timber.i("Read ${enrichedCodes.size} DTCs from ${system.displayName}")
            return Result.Success(enrichedCodes)

        } catch (e: Exception) {
            Timber.e(e, "Failed to read DTCs")
            return Result.Error(e)
        }
    }

    /**
     * Clear DTCs from vehicle.
     * Mode 04 - Clear DTCs and reset MIL
     */
    suspend fun clearDtcCodes(): Result<Unit> {
        try {
            Timber.d("Clearing DTCs")

            val response = elm327Protocol.sendCommand("04")

            if (response == null) {
                return Result.Error(Exception("Failed to clear DTCs"))
            }

            Timber.i("DTCs cleared successfully")
            return Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to clear DTCs")
            return Result.Error(e)
        }
    }

    /**
     * Save diagnostic session to database with freeze frame data.
     */
    suspend fun saveDiagnosticSession(
        vin: String?,
        manufacturer: Manufacturer?,
        codes: List<DiagnosticTroubleCode>,
        freezeFrames: Map<String, FreezeFrameData>? = null,
        notes: String? = null,
        systemScanned: String? = null
    ): Result<Long> {
        try {
            val codesJson = JSONArray(codes.map { it.code }).toString()
            
            // Convert freeze frames to JSON
            val freezeFramesJson = freezeFrames?.let { frames ->
                val json = JSONObject()
                frames.forEach { (code, data) ->
                    json.put(code, data.toJson())
                }
                json.toString()
            }

            val session = DiagnosticSessionEntity(
                timestamp = System.currentTimeMillis(),
                vin = vin,
                manufacturer = manufacturer?.name,
                codes = codesJson,
                freezeFrames = freezeFramesJson,
                notes = notes,
                systemScanned = systemScanned
            )

            val id = sessionDao.insertSession(session)
            Timber.i("Saved diagnostic session: $id (${codes.size} codes, ${freezeFrames?.size ?: 0} freeze frames)")

            return Result.Success(id)

        } catch (e: Exception) {
            Timber.e(e, "Failed to save diagnostic session")
            return Result.Error(e)
        }
    }

    /**
     * Get all diagnostic sessions.
     */
    fun getAllSessions(): Flow<List<DiagnosticSessionEntity>> {
        return sessionDao.getAllSessions()
    }

    /**
     * Get DTC by code from database.
     */
    suspend fun getDtcByCode(code: String): DiagnosticTroubleCode? {
        val entity = dtcDao.getDtcByCode(code) ?: return null

        return DiagnosticTroubleCode(
            code = entity.code,
            description = entity.description,
            system = ECUSystem.valueOf(entity.system),
            severity = Severity.valueOf(entity.severity),
            manufacturer = entity.manufacturer?.let { Manufacturer.fromString(it) }
        )
    }

    /**
     * Parse DTC codes from OBD2 response.
     * Response format: 43 02 XX XX YY YY ...
     * Where 43 = Mode 03 response, 02 = number of codes, XX XX YY YY = DTC bytes
     */
    private fun parseDtcCodes(response: String, system: ECUSystem): List<DiagnosticTroubleCode> {
        val codes = mutableListOf<DiagnosticTroubleCode>()

        try {
            // Remove spaces and split into bytes
            val bytes = response.replace(" ", "")
                .chunked(2)
                .mapNotNull { it.toIntOrNull(16) }

            if (bytes.size < 2) return emptyList()

            // Skip mode byte (43) and get count
            var i = 1
            if (bytes[0] == 0x43) {
                i = 2 // Skip mode and count bytes
            }

            // Parse DTC pairs
            while (i < bytes.size - 1) {
                val byte1 = bytes[i]
                val byte2 = bytes[i + 1]

                val code = decodeDtcBytes(byte1, byte2)
                if (code != null && code != "P0000") { // Ignore null codes
                    codes.add(
                        DiagnosticTroubleCode(
                            code = code,
                            description = "Unknown - check database",
                            system = system,
                            severity = Severity.fromCode(code)
                        )
                    )
                }

                i += 2
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to parse DTC codes")
        }

        return codes
    }

    /**
     * Decode DTC from two bytes.
     * First byte: high nibble = type (P/C/B/U), low nibble = first digit
     * Second byte: two digits
     */
    private fun decodeDtcBytes(byte1: Int, byte2: Int): String? {
        // First two bits determine the type
        val type = when ((byte1 and 0xC0) shr 6) {
            0 -> 'P' // Powertrain
            1 -> 'C' // Chassis
            2 -> 'B' // Body
            3 -> 'U' // Network
            else -> return null
        }

        // Remaining bits form the code number
        val digit1 = (byte1 and 0x30) shr 4
        val digit2 = byte1 and 0x0F
        val digit3 = (byte2 and 0xF0) shr 4
        val digit4 = byte2 and 0x0F

        return "$type$digit1$digit2$digit3$digit4"
    }

    /**
     * Enrich DTC codes with descriptions from database.
     * Looks up manufacturer-specific codes first, then falls back to generic.
     */
    private suspend fun enrichCodesWithDescriptions(
        codes: List<DiagnosticTroubleCode>,
        manufacturer: Manufacturer? = null
    ): List<DiagnosticTroubleCode> {
        return codes.map { code ->
            // Try manufacturer-specific lookup first
            val dbCode = if (manufacturer != null) {
                dtcDao.getDtcByCodeWithManufacturer(code.code, manufacturer.name)
            } else {
                dtcDao.getDtcByCode(code.code)
            }

            if (dbCode != null) {
                code.copy(
                    description = dbCode.description,
                    possibleCauses = dbCode.possibleCauses,
                    manufacturer = dbCode.manufacturer?.let { Manufacturer.fromString(it) }
                )
            } else {
                code
            }
        }
    }

    /**
     * Discover available ECUs in the vehicle.
     */
    suspend fun discoverECUs(): Result<com.cardiag.pro.data.model.ECUDiscoveryResult> {
        return try {
            val result = elm327Protocol.discoverECUs()
            Result.Success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to discover ECUs")
            Result.Error(e)
        }
    }

    /**
     * Read freeze frame data for a specific DTC code.
     * Mode 02 - Request freeze frame data.
     */
    suspend fun readFreezeFrame(dtcCode: String, frameNumber: Int = 0): Result<FreezeFrameData> {
        try {
            Timber.d("Reading freeze frame for $dtcCode (frame $frameNumber)")
            
            // Build freeze frame request for common PIDs
            val command = FreezeFrameParser.buildFreezeFrameRequestWithPIDs(
                frameNumber,
                0x0C, // RPM
                0x0D, // Speed
                0x05, // Coolant temp
                0x11, // Throttle
                0x04, // Engine load
                0x06, // Short term fuel trim
                0x07, // Long term fuel trim
                0x0F, // Intake air temp
                0x10, // MAF
                0x0A  // Fuel pressure
            )
            
            val response = elm327Protocol.sendCommand(command)
            
            if (response == null) {
                return Result.Error(Exception("No response from freeze frame request"))
            }
            
            if (response.contains("NO DATA", ignoreCase = true)) {
                Timber.w("No freeze frame data available for $dtcCode")
                return Result.Error(Exception("Freeze frame not available"))
            }
            
            val freezeFrame = FreezeFrameParser.parseFreezeFrame(dtcCode, response)
            
            if (freezeFrame == null) {
                return Result.Error(Exception("Failed to parse freeze frame data"))
            }
            
            Timber.i("Freeze frame read successfully: ${freezeFrame.getSummary()}")
            return Result.Success(freezeFrame)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to read freeze frame")
            return Result.Error(e)
        }
    }

    /**
     * Read freeze frames for all DTCs.
     */
    suspend fun readAllFreezeFrames(codes: List<DiagnosticTroubleCode>): Map<String, FreezeFrameData> {
        val freezeFrames = mutableMapOf<String, FreezeFrameData>()
        
        for (code in codes) {
            when (val result = readFreezeFrame(code.code)) {
                is Result.Success -> {
                    freezeFrames[code.code] = result.data
                }
                is Result.Error -> {
                    Timber.w("Could not read freeze frame for ${code.code}: ${result.exception.message}")
                }
            }
        }
        
        return freezeFrames
    }

    /**
     * Read DTCs from all available ECUs.
     * Returns a map of ECU to list of DTCs found.
     */
    suspend fun readDtcCodesFromAllECUs(
        manufacturer: Manufacturer? = null
    ): Result<Map<com.cardiag.pro.data.model.ECU, List<DiagnosticTroubleCode>>> {
        try {
            Timber.i("=== Reading DTCs from all ECUs ===")
            
            // First discover which ECUs are available
            val discoveryResult = when (val result = discoverECUs()) {
                is Result.Success -> result.data
                is Result.Error -> return Result.Error(result.exception)
            }
            
            val ecuDtcMap = mutableMapOf<com.cardiag.pro.data.model.ECU, List<DiagnosticTroubleCode>>()
            
            // Read DTCs from each responding ECU
            for (ecuInfo in discoveryResult.availableECUs) {
                if (!ecuInfo.isResponding) continue
                
                Timber.d("Reading DTCs from ${ecuInfo.ecu.displayName}...")
                val response = elm327Protocol.readDTCsFromECU(ecuInfo.ecu)
                
                if (response != null && !response.contains("NO DATA", ignoreCase = true)) {
                    // Map ECU to ECUSystem for compatibility
                    val system = when (ecuInfo.ecu) {
                        com.cardiag.pro.data.model.ECU.ENGINE -> ECUSystem.ENGINE
                        com.cardiag.pro.data.model.ECU.TRANSMISSION -> ECUSystem.TRANSMISSION
                        com.cardiag.pro.data.model.ECU.ABS -> ECUSystem.ABS
                        com.cardiag.pro.data.model.ECU.SRS -> ECUSystem.SRS
                        else -> ECUSystem.ENGINE
                    }
                    
                    val codes = parseDtcCodes(response, system)
                    val enrichedCodes = enrichCodesWithDescriptions(codes, manufacturer)
                        .map { it.copy(sourceECU = ecuInfo.ecu) } // Tag with source ECU
                    
                    if (enrichedCodes.isNotEmpty()) {
                        ecuDtcMap[ecuInfo.ecu] = enrichedCodes
                        Timber.i("${ecuInfo.ecu.displayName}: ${enrichedCodes.size} code(s)")
                    }
                }
            }
            
            val totalCodes = ecuDtcMap.values.sumOf { it.size }
            Timber.i("=== Total: $totalCodes codes from ${ecuDtcMap.size} ECUs ===")
            
            return Result.Success(ecuDtcMap)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to read DTCs from all ECUs")
            return Result.Error(e)
        }
    }

    /**
     * Get statistics about loaded DTC codes.
     */
    suspend fun getDtcStatistics(): DtcStatistics {
        return DtcStatistics(
            total = dtcDao.getCodeCount(),
            generic = dtcDao.getGenericCodeCount(),
            bmw = dtcDao.getManufacturerCodeCount("BMW"),
            vw = dtcDao.getManufacturerCodeCount("VW"),
            nissan = dtcDao.getManufacturerCodeCount("NISSAN")
        )
    }
}

/**
 * Statistics about DTC codes in database.
 */
data class DtcStatistics(
    val total: Int,
    val generic: Int,
    val bmw: Int,
    val vw: Int,
    val nissan: Int
)
