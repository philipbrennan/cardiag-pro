package com.cardiag.pro.data.model

/**
 * Information about an OBD2 adapter (Bluetooth or USB).
 */
data class AdapterInfo(
    val id: String,
    val name: String,
    val type: AdapterType,
    val address: String? = null // MAC address for Bluetooth, USB path for USB
)

enum class AdapterType {
    BLUETOOTH,
    USB,
    BLE
}
