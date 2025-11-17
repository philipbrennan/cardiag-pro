package com.cardiag.pro.data.model

/**
 * Represents a Diagnostic Trouble Code (DTC) with its metadata.
 */
data class DiagnosticTroubleCode(
    val code: String,
    val description: String,
    val system: ECUSystem,
    val severity: Severity,
    val manufacturer: Manufacturer? = null, // null for generic codes
    val possibleCauses: String? = null // Comma-separated list of possible causes
)

/**
 * ECU systems that can be diagnosed.
 */
enum class ECUSystem(val displayName: String, val pid: String) {
    ENGINE("Engine", "01"),
    TRANSMISSION("Transmission", "02"),
    ABS("ABS", "03"),
    SRS("Airbag/SRS", "04");

    companion object {
        fun fromPid(pid: String): ECUSystem? {
            return entries.find { it.pid == pid }
        }
    }
}

/**
 * Severity levels for DTCs.
 */
enum class Severity(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical");

    companion object {
        fun fromCode(code: String): Severity {
            // P0xxx codes - generic powertrain
            // P1xxx codes - manufacturer specific
            // First digit after P indicates severity in some cases
            return when {
                code.startsWith("P03") -> CRITICAL  // Ignition/misfire
                code.startsWith("P04") -> HIGH      // Emissions
                code.startsWith("P05") -> MEDIUM    // Idle/fuel
                code.startsWith("P06") -> HIGH      // Computer/output
                code.startsWith("P07") -> HIGH      // Transmission
                code.startsWith("P08") -> MEDIUM    // Transmission
                code.startsWith("C0") -> HIGH       // Chassis codes
                code.startsWith("B0") -> LOW        // Body codes
                code.startsWith("U0") -> MEDIUM     // Network codes
                else -> MEDIUM
            }
        }
    }
}
