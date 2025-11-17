package com.cardiag.pro.data.model

import org.json.JSONObject

/**
 * Freeze frame data captured at the time of a DTC fault.
 * Represents a snapshot of vehicle sensor values.
 */
data class FreezeFrameData(
    val dtcCode: String,
    val frameNumber: Int = 0, // Frame 0 = most recent
    val rpm: Int? = null,
    val speed: Int? = null,
    val coolantTemp: Int? = null,
    val throttlePosition: Int? = null,
    val engineLoad: Int? = null,
    val shortTermFuelTrim: Double? = null,
    val longTermFuelTrim: Double? = null,
    val intakeAirTemp: Int? = null,
    val mafAirFlow: Double? = null,
    val fuelPressure: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Convert to JSON string for database storage.
     */
    fun toJson(): String {
        val json = JSONObject()
        json.put("dtcCode", dtcCode)
        json.put("frameNumber", frameNumber)
        rpm?.let { json.put("rpm", it) }
        speed?.let { json.put("speed", it) }
        coolantTemp?.let { json.put("coolantTemp", it) }
        throttlePosition?.let { json.put("throttlePosition", it) }
        engineLoad?.let { json.put("engineLoad", it) }
        shortTermFuelTrim?.let { json.put("shortTermFuelTrim", it) }
        longTermFuelTrim?.let { json.put("longTermFuelTrim", it) }
        intakeAirTemp?.let { json.put("intakeAirTemp", it) }
        mafAirFlow?.let { json.put("mafAirFlow", it) }
        fuelPressure?.let { json.put("fuelPressure", it) }
        json.put("timestamp", timestamp)
        return json.toString()
    }

    companion object {
        /**
         * Parse from JSON string stored in database.
         */
        fun fromJson(jsonString: String): FreezeFrameData? {
            return try {
                val json = JSONObject(jsonString)
                FreezeFrameData(
                    dtcCode = json.getString("dtcCode"),
                    frameNumber = json.optInt("frameNumber", 0),
                    rpm = json.optInt("rpm").takeIf { json.has("rpm") },
                    speed = json.optInt("speed").takeIf { json.has("speed") },
                    coolantTemp = json.optInt("coolantTemp").takeIf { json.has("coolantTemp") },
                    throttlePosition = json.optInt("throttlePosition").takeIf { json.has("throttlePosition") },
                    engineLoad = json.optInt("engineLoad").takeIf { json.has("engineLoad") },
                    shortTermFuelTrim = json.optDouble("shortTermFuelTrim").takeIf { json.has("shortTermFuelTrim") },
                    longTermFuelTrim = json.optDouble("longTermFuelTrim").takeIf { json.has("longTermFuelTrim") },
                    intakeAirTemp = json.optInt("intakeAirTemp").takeIf { json.has("intakeAirTemp") },
                    mafAirFlow = json.optDouble("mafAirFlow").takeIf { json.has("mafAirFlow") },
                    fuelPressure = json.optInt("fuelPressure").takeIf { json.has("fuelPressure") },
                    timestamp = json.getLong("timestamp")
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Get a human-readable summary of the freeze frame data.
     */
    fun getSummary(): String {
        val parts = mutableListOf<String>()
        
        rpm?.let { parts.add("RPM: $it") }
        speed?.let { parts.add("Speed: $it km/h") }
        coolantTemp?.let { parts.add("Coolant: $it°C") }
        throttlePosition?.let { parts.add("Throttle: $it%") }
        engineLoad?.let { parts.add("Load: $it%") }
        
        return if (parts.isEmpty()) {
            "No freeze frame data available"
        } else {
            parts.joinToString(", ")
        }
    }
}
