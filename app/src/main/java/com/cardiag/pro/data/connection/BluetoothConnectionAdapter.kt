package com.cardiag.pro.data.connection

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.cardiag.pro.data.model.AdapterInfo
import com.cardiag.pro.data.model.AdapterType
import com.cardiag.pro.data.model.ConnectionState
import com.cardiag.pro.data.model.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bluetooth Classic connection adapter for ELM327 OBD2 adapters.
 * Implements the ConnectionAdapter interface for Bluetooth SPP connections.
 */
@Singleton
class BluetoothConnectionAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : ConnectionAdapter {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    companion object {
        // Standard Serial Port Profile UUID for Bluetooth SPP
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val BUFFER_SIZE = 1024
    }

    init {
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                Timber.w("Bluetooth not supported on this device")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Bluetooth adapter")
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun scanForAdapters(): Result<List<AdapterInfo>> = withContext(Dispatchers.IO) {
        try {
            if (!hasBluetoothPermissions()) {
                return@withContext Result.Error(SecurityException("Bluetooth permissions not granted"))
            }

            val adapter = bluetoothAdapter
                ?: return@withContext Result.Error(IllegalStateException("Bluetooth not available"))

            if (!adapter.isEnabled) {
                return@withContext Result.Error(IllegalStateException("Bluetooth is disabled"))
            }

            _connectionState.value = ConnectionState.Scanning

            // Get paired devices
            val pairedDevices = adapter.bondedDevices ?: emptySet()
            val adapters = pairedDevices
                .filter { isLikelyObd2Device(it.name) }
                .map { device ->
                    AdapterInfo(
                        id = device.address,
                        name = device.name ?: "Unknown Device",
                        type = AdapterType.BLUETOOTH,
                        address = device.address
                    )
                }

            _connectionState.value = ConnectionState.Disconnected
            Timber.d("Found ${adapters.size} Bluetooth OBD2 adapters")
            Result.Success(adapters)
        } catch (e: Exception) {
            Timber.e(e, "Failed to scan for Bluetooth adapters")
            _connectionState.value = ConnectionState.Error(e.message ?: "Scan failed", e)
            Result.Error(e as? Exception ?: Exception(e))
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(adapterInfo: AdapterInfo): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!hasBluetoothPermissions()) {
                return@withContext Result.Error(SecurityException("Bluetooth permissions not granted"))
            }

            _connectionState.value = ConnectionState.Connecting(adapterInfo)

            // Disconnect existing connection if any
            disconnect()

            val adapter = bluetoothAdapter
                ?: return@withContext Result.Error(IllegalStateException("Bluetooth not available"))

            val device: BluetoothDevice = adapter.getRemoteDevice(adapterInfo.address)
                ?: return@withContext Result.Error(IllegalArgumentException("Invalid device address"))

            Timber.d("Connecting to Bluetooth device: ${device.name} (${device.address})")

            // Create and connect socket
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothSocket?.connect()

            // Get streams
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream

            _connectionState.value = ConnectionState.Connected(adapterInfo)
            Timber.i("Connected to Bluetooth adapter: ${adapterInfo.name}")
            Result.Success(Unit)
        } catch (e: IOException) {
            Timber.e(e, "Failed to connect to Bluetooth adapter")
            disconnect()
            _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}", e)
            Result.Error(e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during Bluetooth connection")
            disconnect()
            _connectionState.value = ConnectionState.Error("Unexpected error: ${e.message}", e)
            Result.Error(e as? Exception ?: Exception(e))
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            Timber.d("Disconnecting Bluetooth adapter")
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()

            inputStream = null
            outputStream = null
            bluetoothSocket = null

            _connectionState.value = ConnectionState.Disconnected
            Timber.i("Bluetooth adapter disconnected")
        } catch (e: IOException) {
            Timber.e(e, "Error during disconnect")
        }
    }

    override suspend fun sendCommand(command: String, timeoutMs: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected()) {
                return@withContext Result.Error(IllegalStateException("Not connected"))
            }

            val output = outputStream ?: return@withContext Result.Error(IllegalStateException("Output stream not available"))
            val input = inputStream ?: return@withContext Result.Error(IllegalStateException("Input stream not available"))

            // Send command with carriage return
            val commandBytes = (command + "\r").toByteArray(Charsets.US_ASCII)
            Timber.d("Sending command: $command")
            output.write(commandBytes)
            output.flush()

            // Read response with timeout
            val response = withTimeoutOrNull(timeoutMs) {
                readResponse(input)
            }

            if (response == null) {
                Timber.e("Command timeout after ${timeoutMs}ms")
                return@withContext Result.Error(IOException("Command timeout"))
            }

            Timber.d("Received response: $response")
            Result.Success(response)
        } catch (e: IOException) {
            Timber.e(e, "Failed to send command")
            disconnect()
            Result.Error(e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error sending command")
            Result.Error(e as? Exception ?: Exception(e))
        }
    }

    override fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }

    /**
     * Read response from input stream until '>' prompt.
     */
    private suspend fun readResponse(input: InputStream): String = withContext(Dispatchers.IO) {
        val buffer = ByteArray(BUFFER_SIZE)
        val response = StringBuilder()

        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead > 0) {
                val chunk = String(buffer, 0, bytesRead, Charsets.US_ASCII)
                response.append(chunk)

                // ELM327 responses end with '>'
                if (response.contains(">")) {
                    break
                }
            }
        }

        response.toString().trim()
    }

    /**
     * Check if device name suggests it's an OBD2 adapter.
     */
    private fun isLikelyObd2Device(name: String?): Boolean {
        if (name == null) return false
        val lowerName = name.lowercase()
        return lowerName.contains("obd") ||
                lowerName.contains("elm") ||
                lowerName.contains("vlink") ||
                lowerName.contains("vgate") ||
                lowerName.contains("scantool")
    }

    /**
     * Check if app has necessary Bluetooth permissions.
     */
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11 and below
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }
}
