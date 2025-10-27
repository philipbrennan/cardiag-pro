package com.cardiag.pro.data.model

/**
 * Represents the current state of the OBD2 adapter connection.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    data class Connecting(val adapterInfo: AdapterInfo) : ConnectionState()
    data class Connected(val adapterInfo: AdapterInfo) : ConnectionState()
    data class Error(val message: String, val exception: Throwable? = null) : ConnectionState()
}
