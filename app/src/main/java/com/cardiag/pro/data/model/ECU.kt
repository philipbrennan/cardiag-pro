package com.cardiag.pro.data.model

/**
 * Represents an Electronic Control Unit in the vehicle.
 */
enum class ECU(
    val displayName: String,
    val header: String,
    val description: String
) {
    ENGINE("Engine", "7E0", "Engine Control Module (ECM)"),
    TRANSMISSION("Transmission", "7E1", "Transmission Control Module (TCM)"),
    ABS("ABS", "7E2", "Anti-lock Braking System"),
    SRS("Airbag", "7E3", "Supplemental Restraint System"),
    BCM("Body", "7E4", "Body Control Module"),
    HVAC("HVAC", "7E5", "Climate Control"),
    INSTRUMENT("Instrument", "7E6", "Instrument Cluster"),
    IMMOBILIZER("Security", "7E7", "Immobilizer/Security");

    companion object {
        /**
         * Get ECU from header address.
         */
        fun fromHeader(header: String): ECU? {
            return entries.find { it.header.equals(header, ignoreCase = true) }
        }

        /**
         * Get default ECU for backward compatibility (Engine).
         */
        fun default(): ECU = ENGINE
    }
}

/**
 * Information about an ECU discovered in the vehicle.
 */
data class ECUInfo(
    val ecu: ECU,
    val isResponding: Boolean,
    val protocolSupported: Boolean = true,
    val lastResponseTime: Long = System.currentTimeMillis()
)

/**
 * Result of ECU discovery scan.
 */
data class ECUDiscoveryResult(
    val availableECUs: List<ECUInfo>,
    val totalScanned: Int,
    val scanDurationMs: Long
)
