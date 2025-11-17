package com.cardiag.pro.data.model

/**
 * Represents live sensor data from OBD2 Mode 01 requests.
 */
data class LiveSensorData(
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
    val intakeManifoldPressure: Int? = null,
    val timingAdvance: Double? = null,
    val fuelLevel: Int? = null,
    val batteryVoltage: Double? = null,
    val ambientAirTemp: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if any sensor data is available.
     */
    fun hasData(): Boolean {
        return rpm != null || speed != null || coolantTemp != null || 
               throttlePosition != null || engineLoad != null
    }

    /**
     * Get a summary of key parameters.
     */
    fun getKeySummary(): String {
        val parts = mutableListOf<String>()
        rpm?.let { parts.add("RPM: $it") }
        speed?.let { parts.add("$it km/h") }
        coolantTemp?.let { parts.add("$it°C") }
        return parts.joinToString(" | ")
    }
}

/**
 * Supported OBD2 PIDs for live data monitoring.
 */
enum class ObdPid(
    val pid: Int,
    val displayName: String,
    val unit: String,
    val bytes: Int
) {
    ENGINE_RPM(0x0C, "Engine RPM", "rpm", 2),
    VEHICLE_SPEED(0x0D, "Vehicle Speed", "km/h", 1),
    COOLANT_TEMP(0x05, "Coolant Temp", "°C", 1),
    THROTTLE_POSITION(0x11, "Throttle Position", "%", 1),
    ENGINE_LOAD(0x04, "Engine Load", "%", 1),
    SHORT_TERM_FUEL_TRIM(0x06, "Short Term Fuel Trim", "%", 1),
    LONG_TERM_FUEL_TRIM(0x07, "Long Term Fuel Trim", "%", 1),
    INTAKE_AIR_TEMP(0x0F, "Intake Air Temp", "°C", 1),
    MAF_AIR_FLOW(0x10, "MAF Air Flow", "g/s", 2),
    FUEL_PRESSURE(0x0A, "Fuel Pressure", "kPa", 1),
    INTAKE_MANIFOLD_PRESSURE(0x0B, "Intake Pressure", "kPa", 1),
    TIMING_ADVANCE(0x0E, "Timing Advance", "° BTDC", 1),
    FUEL_LEVEL(0x2F, "Fuel Level", "%", 1),
    AMBIENT_AIR_TEMP(0x46, "Ambient Air Temp", "°C", 1);

    /**
     * Get the hex command string for this PID.
     */
    fun getCommand(): String {
        return "01${pid.toString(16).padStart(2, '0').uppercase()}"
    }

    companion object {
        /**
         * Get common PIDs for monitoring dashboard.
         */
        fun getCommonPids(): List<ObdPid> {
            return listOf(
                ENGINE_RPM,
                VEHICLE_SPEED,
                COOLANT_TEMP,
                THROTTLE_POSITION,
                ENGINE_LOAD,
                SHORT_TERM_FUEL_TRIM,
                LONG_TERM_FUEL_TRIM,
                MAF_AIR_FLOW,
                INTAKE_MANIFOLD_PRESSURE,
                TIMING_ADVANCE
            )
        }

        /**
         * Find PID by hex value.
         */
        fun fromPid(pid: Int): ObdPid? {
            return entries.find { it.pid == pid }
        }
    }
}
