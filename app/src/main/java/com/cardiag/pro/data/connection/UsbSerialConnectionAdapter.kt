package com.cardiag.pro.data.connection

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.cardiag.pro.data.model.AdapterInfo
import com.cardiag.pro.data.model.AdapterType
import com.cardiag.pro.data.model.ConnectionState
import com.cardiag.pro.data.model.Result
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * USB Serial connection adapter for FT232-based OBD2 adapters.
 * Implements the ConnectionAdapter interface for USB serial connections.
 */
@Singleton
class UsbSerialConnectionAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : ConnectionAdapter {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var usbManager: UsbManager? = null
    private var serialPort: UsbSerialPort? = null
    private var currentDriver: UsbSerialDriver? = null

    companion object {
        private const val BAUD_RATE = 38400 // Standard ELM327 baud rate
        private const val DATA_BITS = 8
        private const val STOP_BITS = UsbSerialPort.STOPBITS_1
        private const val PARITY = UsbSerialPort.PARITY_NONE
        private const val READ_TIMEOUT_MS = 5000
        private const val WRITE_TIMEOUT_MS = 5000
        private const val ACTION_USB_PERMISSION = "com.cardiag.pro.USB_PERMISSION"
    }

    init {
        usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        if (usbManager == null) {
            Timber.w("USB not supported on this device")
        }
    }

    override suspend fun scanForAdapters(): Result<List<AdapterInfo>> = withContext(Dispatchers.IO) {
        try {
            val manager = usbManager
                ?: return@withContext Result.Error(IllegalStateException("USB not available"))

            _connectionState.value = ConnectionState.Scanning

            // Find all available USB serial devices
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)

            val adapters = availableDrivers.map { driver ->
                val device = driver.device
                AdapterInfo(
                    id = "${device.vendorId}:${device.productId}",
                    name = device.deviceName ?: "USB Serial Device",
                    type = AdapterType.USB,
                    address = device.deviceName
                )
            }

            _connectionState.value = ConnectionState.Disconnected
            Timber.d("Found ${adapters.size} USB serial adapters")
            Result.Success(adapters)
        } catch (e: Exception) {
            Timber.e(e, "Failed to scan for USB adapters")
            _connectionState.value = ConnectionState.Error(e.message ?: "Scan failed", e)
            Result.Error(e as? Exception ?: Exception(e))
        }
    }

    override suspend fun connect(adapterInfo: AdapterInfo): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting(adapterInfo)

            // Disconnect existing connection if any
            disconnect()

            val manager = usbManager
                ?: return@withContext Result.Error(IllegalStateException("USB not available"))

            // Find the USB device
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
            val driver = availableDrivers.firstOrNull { driver ->
                val device = driver.device
                "${device.vendorId}:${device.productId}" == adapterInfo.id
            } ?: return@withContext Result.Error(IllegalArgumentException("USB device not found"))

            val device = driver.device

            // Request permission if needed
            if (!manager.hasPermission(device)) {
                Timber.d("Requesting USB permission for ${device.deviceName}")
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val permissionIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(ACTION_USB_PERMISSION),
                    flags
                )
                manager.requestPermission(device, permissionIntent)
                return@withContext Result.Error(SecurityException("USB permission required"))
            }

            // Open connection
            val connection = manager.openDevice(device)
                ?: return@withContext Result.Error(IOException("Failed to open USB device"))

            Timber.d("Opening USB serial port for ${device.deviceName}")

            // Get first port (most devices have only one)
            val port = driver.ports.firstOrNull()
                ?: return@withContext Result.Error(IllegalStateException("No USB serial ports available"))

            port.open(connection)
            port.setParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY)

            serialPort = port
            currentDriver = driver

            _connectionState.value = ConnectionState.Connected(adapterInfo)
            Timber.i("Connected to USB adapter: ${adapterInfo.name}")
            Result.Success(Unit)
        } catch (e: IOException) {
            Timber.e(e, "Failed to connect to USB adapter")
            disconnect()
            _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}", e)
            Result.Error(e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during USB connection")
            disconnect()
            _connectionState.value = ConnectionState.Error("Unexpected error: ${e.message}", e)
            Result.Error(e as? Exception ?: Exception(e))
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            Timber.d("Disconnecting USB adapter")
            serialPort?.close()
            serialPort = null
            currentDriver = null

            _connectionState.value = ConnectionState.Disconnected
            Timber.i("USB adapter disconnected")
        } catch (e: IOException) {
            Timber.e(e, "Error during disconnect")
        }
    }

    override suspend fun sendCommand(command: String, timeoutMs: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected()) {
                return@withContext Result.Error(IllegalStateException("Not connected"))
            }

            val port = serialPort
                ?: return@withContext Result.Error(IllegalStateException("Serial port not available"))

            // Send command with carriage return
            val commandBytes = (command + "\r").toByteArray(Charsets.US_ASCII)
            Timber.d("Sending command: $command")
            port.write(commandBytes, WRITE_TIMEOUT_MS)

            // Read response with timeout
            val response = withTimeoutOrNull(timeoutMs) {
                readResponse(port)
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
        return serialPort?.isOpen == true
    }

    /**
     * Read response from serial port until '>' prompt.
     */
    private suspend fun readResponse(port: UsbSerialPort): String = withContext(Dispatchers.IO) {
        val buffer = ByteArray(1024)
        val response = StringBuilder()

        while (true) {
            val bytesRead = port.read(buffer, READ_TIMEOUT_MS)
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
}
