package com.cardiag.pro.data.parser

import com.cardiag.pro.data.model.FreezeFrameData
import timber.log.Timber

/**
 * Parses OBD2 Mode 02 (freeze frame) responses.
 */
object FreezeFrameParser {

    /**
     * Parse freeze frame data from Mode 02 response.
     * 
     * Format: 42 [frame] [PID] [data bytes]
     * Example: 42 00 0C 1A 4F  = Frame 0, PID 0C (RPM), data 1A4F
     */
    fun parseFreezeFrame(dtcCode: String, response: String): FreezeFrameData? {
        try {
            Timber.d("Parsing freeze frame for $dtcCode: $response")
            
            // Remove spaces and convert to bytes
            val hexString = response.replace(Regex("\\s+"), "")
            val hexPairs = hexString.chunked(2)
            
            // Check for invalid hex before parsing
            if (hexPairs.any { it.length == 2 && it.toIntOrNull(16) == null }) {
                Timber.e("Invalid hex characters in response")
                return null
            }
            
            val bytes = hexPairs.mapNotNull { it.toIntOrNull(16) }
            
            if (bytes.isEmpty()) {
                Timber.e("Empty byte array from response")
                return null
            }
            
            // Check for Mode 02 response (42)
            if (bytes[0] != 0x42) {
                Timber.w("Not a Mode 02 response: ${bytes[0].toString(16)}")
                return null
            }
            
            val frameNumber = if (bytes.size > 1) bytes[1] else 0
            Timber.d("Frame number: $frameNumber")
            
            // Parse PIDs from the response
            var rpm: Int? = null
            var speed: Int? = null
            var coolantTemp: Int? = null
            var throttlePosition: Int? = null
            var engineLoad: Int? = null
            var shortTermFuelTrim: Double? = null
            var longTermFuelTrim: Double? = null
            var intakeAirTemp: Int? = null
            var mafAirFlow: Double? = null
            var fuelPressure: Int? = null
            
            var i = 2 // Start after mode byte and frame number
            while (i < bytes.size) {
                if (i + 1 >= bytes.size) break
                
                val pid = bytes[i]
                Timber.d("Found PID: ${pid.toString(16).padStart(2, '0')}")
                
                when (pid) {
                    0x0C -> { // RPM (2 bytes)
                        if (i + 2 < bytes.size) {
                            rpm = ((bytes[i + 1] * 256 + bytes[i + 2]) / 4)
                            Timber.d("RPM: $rpm")
                            i += 3
                        } else i++
                    }
                    0x0D -> { // Speed (1 byte)
                        if (i + 1 < bytes.size) {
                            speed = bytes[i + 1]
                            Timber.d("Speed: $speed km/h")
                            i += 2
                        } else i++
                    }
                    0x05 -> { // Coolant temp (1 byte)
                        if (i + 1 < bytes.size) {
                            coolantTemp = bytes[i + 1] - 40
                            Timber.d("Coolant temp: $coolantTemp°C")
                            i += 2
                        } else i++
                    }
                    0x11 -> { // Throttle position (1 byte)
                        if (i + 1 < bytes.size) {
                            throttlePosition = (bytes[i + 1] * 100) / 255
                            Timber.d("Throttle: $throttlePosition%")
                            i += 2
                        } else i++
                    }
                    0x04 -> { // Engine load (1 byte)
                        if (i + 1 < bytes.size) {
                            engineLoad = (bytes[i + 1] * 100) / 255
                            Timber.d("Engine load: $engineLoad%")
                            i += 2
                        } else i++
                    }
                    0x06 -> { // Short term fuel trim (1 byte)
                        if (i + 1 < bytes.size) {
                            shortTermFuelTrim = (bytes[i + 1] - 128) * (100.0 / 128.0)
                            Timber.d("Short term fuel trim: $shortTermFuelTrim%")
                            i += 2
                        } else i++
                    }
                    0x07 -> { // Long term fuel trim (1 byte)
                        if (i + 1 < bytes.size) {
                            longTermFuelTrim = (bytes[i + 1] - 128) * (100.0 / 128.0)
                            Timber.d("Long term fuel trim: $longTermFuelTrim%")
                            i += 2
                        } else i++
                    }
                    0x0F -> { // Intake air temp (1 byte)
                        if (i + 1 < bytes.size) {
                            intakeAirTemp = bytes[i + 1] - 40
                            Timber.d("Intake air temp: $intakeAirTemp°C")
                            i += 2
                        } else i++
                    }
                    0x10 -> { // MAF air flow (2 bytes)
                        if (i + 2 < bytes.size) {
                            mafAirFlow = ((bytes[i + 1] * 256 + bytes[i + 2]) / 100.0)
                            Timber.d("MAF: $mafAirFlow g/s")
                            i += 3
                        } else i++
                    }
                    0x0A -> { // Fuel pressure (1 byte)
                        if (i + 1 < bytes.size) {
                            fuelPressure = bytes[i + 1] * 3
                            Timber.d("Fuel pressure: $fuelPressure kPa")
                            i += 2
                        } else i++
                    }
                    else -> {
                        Timber.d("Unknown PID ${pid.toString(16)}, skipping")
                        i++
                    }
                }
            }
            
            // Return null only if we couldn't even read the frame number properly
            // or if there were no PIDs in the response at all
            if (bytes.size <= 2) {
                Timber.w("No PID data in freeze frame response")
                return null
            }
            
            return FreezeFrameData(
                dtcCode = dtcCode,
                frameNumber = frameNumber,
                rpm = rpm,
                speed = speed,
                coolantTemp = coolantTemp,
                throttlePosition = throttlePosition,
                engineLoad = engineLoad,
                shortTermFuelTrim = shortTermFuelTrim,
                longTermFuelTrim = longTermFuelTrim,
                intakeAirTemp = intakeAirTemp,
                mafAirFlow = mafAirFlow,
                fuelPressure = fuelPressure
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse freeze frame data")
            return null
        }
    }

    /**
     * Request freeze frame for a specific DTC.
     * Returns the command string to send.
     */
    fun buildFreezeFrameRequest(dtcCode: String, frameNumber: Int = 0): String {
        // Mode 02, Frame number, then PIDs to request
        // Request common PIDs: 0C (RPM), 0D (Speed), 05 (Coolant), 11 (Throttle), 04 (Load)
        return "02${frameNumber.toString(16).padStart(2, '0')}"
    }

    /**
     * Build a request for specific PIDs in a freeze frame.
     */
    fun buildFreezeFrameRequestWithPIDs(frameNumber: Int = 0, vararg pids: Int): String {
        val pidString = pids.joinToString("") { it.toString(16).padStart(2, '0') }
        return "02${frameNumber.toString(16).padStart(2, '0')}$pidString"
    }
}
