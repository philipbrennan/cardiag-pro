package com.cardiag.pro.data.model

/**
 * Vehicle information decoded from VIN or manually entered.
 */
data class VehicleInfo(
    val vin: String,
    val manufacturer: Manufacturer,
    val year: Int?,
    val model: String?,
    val isManuallyEntered: Boolean = false
)

enum class Manufacturer(val displayName: String) {
    BMW("BMW"),
    VOLKSWAGEN("Volkswagen"),
    NISSAN("Nissan"),
    UNKNOWN("Unknown");

    companion object {
        fun fromVin(vin: String): Manufacturer {
            if (vin.length < 3) return UNKNOWN

            // World Manufacturer Identifier (WMI) - first 3 characters
            val wmi = vin.substring(0, 3).uppercase()

            return when {
                // BMW codes
                wmi.startsWith("WBA") || wmi.startsWith("WBS") ||
                wmi.startsWith("WBY") || wmi.startsWith("4US") ||
                wmi.startsWith("5UX") -> BMW

                // Volkswagen codes
                wmi.startsWith("WVW") || wmi.startsWith("WV1") ||
                wmi.startsWith("WV2") || wmi.startsWith("3VW") -> VOLKSWAGEN

                // Nissan codes
                wmi.startsWith("JN1") || wmi.startsWith("JN8") ||
                wmi.startsWith("1N4") || wmi.startsWith("1N6") -> NISSAN

                else -> UNKNOWN
            }
        }

        fun fromString(value: String): Manufacturer {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
