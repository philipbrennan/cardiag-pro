package com.cardiag.pro.data.connection

import com.cardiag.pro.data.model.AdapterInfo
import com.cardiag.pro.data.model.ConnectionState
import com.cardiag.pro.data.model.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for OBD2 adapter connections (Bluetooth, USB, BLE).
 * Provides a unified API regardless of connection type.
 */
interface ConnectionAdapter {

    /**
     * Current connection state as a flow.
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Scan for available adapters.
     * @return List of discovered adapters
     */
    suspend fun scanForAdapters(): Result<List<AdapterInfo>>

    /**
     * Connect to a specific adapter.
     * @param adapterInfo The adapter to connect to
     * @return Success or Error result
     */
    suspend fun connect(adapterInfo: AdapterInfo): Result<Unit>

    /**
     * Disconnect from the current adapter.
     */
    suspend fun disconnect()

    /**
     * Send a command to the adapter and receive a response.
     * @param command The ELM327 command to send
     * @param timeoutMs Timeout in milliseconds (default 5000ms)
     * @return Response string or Error
     */
    suspend fun sendCommand(command: String, timeoutMs: Long = 5000): Result<String>

    /**
     * Check if currently connected.
     */
    fun isConnected(): Boolean
}
