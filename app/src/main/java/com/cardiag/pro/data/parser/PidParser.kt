package com.cardiag.pro.data.parser

import com.cardiag.pro.data.model.LiveSensorData
import com.cardiag.pro.data.model.ObdPid
import timber.log.Timber

/**
 * Parses OBD2 Mode 01 (live data) responses.
 */
object PidParser {

    /**
     * Parse a Mode 01 response for a specific PID.
     * Response format: 41 [PID] [data bytes]
     * Example: 41 0C 1A 4F = Mode 01 response, PID 0C (RPM), data 1A4F
     */
    fun parseResponse(response: String): Map<ObdPid, Any> {
        val results = mutableMapOf<ObdPid, Any>()
        
        try {
            // Remove spaces and convert to bytes
            val hexString = response.replace(Regex("\\s+"), "")
            val bytes = hexString.chunked(2).mapNotNull { it.toIntOrNull(16) }
            
            if (bytes.isEmpty() || bytes[0] != 0x41) {
                Timber.w("Not a Mode 01 response: $response")
                return emptyMap()
            }
            
            var i = 1
            while (i < bytes.size) {
                if (i >= bytes.size) break
                
                val pidByte = bytes[i]
                val pid = ObdPid.fromPid(pidByte)
                
                if (pid != null && i + pid.bytes < bytes.size) {
                    val value = parsePidValue(pid, bytes.subList(i + 1, i + 1 + pid.bytes))
                    if (value != null) {
                        results[pid] = value
                        Timber.d("Parsed ${pid.displayName}: $value ${pid.unit}")
                    }
                    i += 1 + pid.bytes
                } else {
                    i++
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse PID response")
        }
        
        return results
    }

    /**
     * Parse the value for a specific PID based on its formula.
     */
    private fun parsePidValue(pid: ObdPid, dataBytes: List<Int>): Any? {
        return try {
            when (pid) {
                ObdPid.ENGINE_RPM -> {
                    // ((A * 256) + B) / 4
                    if (dataBytes.size >= 2) {
                        ((dataBytes[0] * 256 + dataBytes[1]) / 4)
                    } else null
                }
                ObdPid.VEHICLE_SPEED -> {
                    // A (km/h)
                    dataBytes[0]
                }
                ObdPid.COOLANT_TEMP, ObdPid.INTAKE_AIR_TEMP, ObdPid.AMBIENT_AIR_TEMP -> {
                    // A - 40 (°C)
                    dataBytes[0] - 40
                }
                ObdPid.THROTTLE_POSITION, ObdPid.ENGINE_LOAD, ObdPid.FUEL_LEVEL -> {
                    // (A * 100) / 255 (%)
                    (dataBytes[0] * 100) / 255
                }
                ObdPid.SHORT_TERM_FUEL_TRIM, ObdPid.LONG_TERM_FUEL_TRIM -> {
                    // ((A - 128) * 100) / 128 (%)
                    ((dataBytes[0] - 128) * 100.0) / 128.0
                }
                ObdPid.MAF_AIR_FLOW -> {
                    // ((A * 256) + B) / 100 (g/s)
                    if (dataBytes.size >= 2) {
                        ((dataBytes[0] * 256 + dataBytes[1]) / 100.0)
                    } else null
                }
                ObdPid.FUEL_PRESSURE -> {
                    // A * 3 (kPa)
                    dataBytes[0] * 3
                }
                ObdPid.INTAKE_MANIFOLD_PRESSURE -> {
                    // A (kPa)
                    dataBytes[0]
                }
                ObdPid.TIMING_ADVANCE -> {
                    // (A - 128) / 2 (degrees before TDC)
                    (dataBytes[0] - 128) / 2.0
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse value for ${pid.displayName}")
            null
        }
    }

    /**
     * Create LiveSensorData from parsed PID results.
     */
    fun toLiveSensorData(pidResults: Map<ObdPid, Any>): LiveSensorData {
        return LiveSensorData(
            rpm = pidResults[ObdPid.ENGINE_RPM] as? Int,
            speed = pidResults[ObdPid.VEHICLE_SPEED] as? Int,
            coolantTemp = pidResults[ObdPid.COOLANT_TEMP] as? Int,
            throttlePosition = pidResults[ObdPid.THROTTLE_POSITION] as? Int,
            engineLoad = pidResults[ObdPid.ENGINE_LOAD] as? Int,
            shortTermFuelTrim = pidResults[ObdPid.SHORT_TERM_FUEL_TRIM] as? Double,
            longTermFuelTrim = pidResults[ObdPid.LONG_TERM_FUEL_TRIM] as? Double,
            intakeAirTemp = pidResults[ObdPid.INTAKE_AIR_TEMP] as? Int,
            mafAirFlow = pidResults[ObdPid.MAF_AIR_FLOW] as? Double,
            fuelPressure = pidResults[ObdPid.FUEL_PRESSURE] as? Int,
            intakeManifoldPressure = pidResults[ObdPid.INTAKE_MANIFOLD_PRESSURE] as? Int,
            timingAdvance = pidResults[ObdPid.TIMING_ADVANCE] as? Double,
            fuelLevel = pidResults[ObdPid.FUEL_LEVEL] as? Int,
            ambientAirTemp = pidResults[ObdPid.AMBIENT_AIR_TEMP] as? Int
        )
    }

    /**
     * Build a multi-PID request command.
     * Example: "010C0D05" requests RPM, Speed, and Coolant Temp in one command.
     */
    fun buildMultiPidRequest(pids: List<ObdPid>): String {
        val pidHex = pids.joinToString("") { 
            it.pid.toString(16).padStart(2, '0').uppercase()
        }
        return "01$pidHex"
    }
}
