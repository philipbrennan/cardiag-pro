package com.cardiag.pro.data.connection

import com.cardiag.pro.data.model.AdapterInfo
import com.cardiag.pro.data.model.AdapterType
import com.cardiag.pro.data.model.ConnectionState
import com.cardiag.pro.data.model.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages connections to OBD2 adapters (Bluetooth, USB).
 * Provides a unified interface for connection management.
 */
@Singleton
class ConnectionManager @Inject constructor(
    private val bluetoothAdapter: BluetoothConnectionAdapter,
    private val usbSerialAdapter: UsbSerialConnectionAdapter
) {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentAdapter: ConnectionAdapter? = null
    private var currentAdapterInfo: AdapterInfo? = null

    /**
     * Scan for available adapters of a specific type.
     */
    suspend fun scanForAdapters(type: AdapterType): Result<List<AdapterInfo>> {
        Timber.d("Scanning for $type adapters")
        return when (type) {
            AdapterType.BLUETOOTH -> {
                _connectionState.value = ConnectionState.Scanning
                bluetoothAdapter.scanForAdapters().also {
                    if (it.isSuccess) {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                }
            }
            AdapterType.USB -> {
                _connectionState.value = ConnectionState.Scanning
                usbSerialAdapter.scanForAdapters().also {
                    if (it.isSuccess) {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                }
            }
            AdapterType.BLE -> {
                // BLE support to be implemented in future iteration
                Result.Error(UnsupportedOperationException("BLE not yet supported"))
            }
        }
    }

    /**
     * Connect to a specific adapter.
     */
    suspend fun connect(adapterInfo: AdapterInfo): Result<Unit> {
        Timber.d("Connecting to ${adapterInfo.type} adapter: ${adapterInfo.name}")

        // Disconnect any existing connection
        disconnect()

        _connectionState.value = ConnectionState.Connecting(adapterInfo)

        val adapter = when (adapterInfo.type) {
            AdapterType.BLUETOOTH -> bluetoothAdapter
            AdapterType.USB -> usbSerialAdapter
            AdapterType.BLE -> {
                _connectionState.value = ConnectionState.Error("BLE not yet supported")
                return Result.Error(UnsupportedOperationException("BLE not yet supported"))
            }
        }

        return adapter.connect(adapterInfo).also { result ->
            when (result) {
                is Result.Success -> {
                    currentAdapter = adapter
                    currentAdapterInfo = adapterInfo
                    _connectionState.value = ConnectionState.Connected(adapterInfo)
                    Timber.i("Successfully connected to ${adapterInfo.name}")
                }
                is Result.Error -> {
                    currentAdapter = null
                    currentAdapterInfo = null
                    _connectionState.value = ConnectionState.Error(
                        result.exception.message ?: "Connection failed",
                        result.exception
                    )
                    Timber.e(result.exception, "Failed to connect to ${adapterInfo.name}")
                }
            }
        }
    }

    /**
     * Disconnect from the current adapter.
     */
    suspend fun disconnect() {
        currentAdapter?.disconnect()
        currentAdapter = null
        currentAdapterInfo = null
        _connectionState.value = ConnectionState.Disconnected
        Timber.d("Disconnected from adapter")
    }

    /**
     * Send a command to the connected adapter.
     */
    suspend fun sendCommand(command: String, timeoutMs: Long = 5000): Result<String> {
        val adapter = currentAdapter
            ?: return Result.Error(IllegalStateException("Not connected to any adapter"))

        return adapter.sendCommand(command, timeoutMs)
    }

    /**
     * Check if currently connected to an adapter.
     */
    fun isConnected(): Boolean {
        return currentAdapter?.isConnected() == true
    }

    /**
     * Get the currently connected adapter info.
     */
    fun getCurrentAdapterInfo(): AdapterInfo? {
        return currentAdapterInfo
    }
}
