package com.cardiag.pro.data.connection

import com.cardiag.pro.data.model.AdapterInfo
import com.cardiag.pro.data.model.AdapterType
import com.cardiag.pro.data.model.ConnectionState
import com.cardiag.pro.data.model.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConnectionManagerTest {

    private lateinit var bluetoothAdapter: BluetoothConnectionAdapter
    private lateinit var usbSerialAdapter: UsbSerialConnectionAdapter
    private lateinit var connectionManager: ConnectionManager

    @BeforeEach
    fun setup() {
        bluetoothAdapter = mockk(relaxed = true)
        usbSerialAdapter = mockk(relaxed = true)
        connectionManager = ConnectionManager(bluetoothAdapter, usbSerialAdapter)
    }

    @Test
    fun `scanForAdapters - bluetooth - delegates to bluetooth adapter`() = runTest {
        // Given
        val expectedAdapters = listOf(
            AdapterInfo("1", "OBD2 Adapter", AdapterType.BLUETOOTH, "00:11:22:33:44:55")
        )
        coEvery { bluetoothAdapter.scanForAdapters() } returns Result.Success(expectedAdapters)

        // When
        val result = connectionManager.scanForAdapters(AdapterType.BLUETOOTH)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedAdapters, (result as Result.Success).data)
        coVerify { bluetoothAdapter.scanForAdapters() }
    }

    @Test
    fun `scanForAdapters - usb - delegates to usb adapter`() = runTest {
        // Given
        val expectedAdapters = listOf(
            AdapterInfo("2", "USB Serial", AdapterType.USB, "/dev/ttyUSB0")
        )
        coEvery { usbSerialAdapter.scanForAdapters() } returns Result.Success(expectedAdapters)

        // When
        val result = connectionManager.scanForAdapters(AdapterType.USB)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedAdapters, (result as Result.Success).data)
        coVerify { usbSerialAdapter.scanForAdapters() }
    }

    @Test
    fun `scanForAdapters - ble - returns unsupported error`() = runTest {
        // When
        val result = connectionManager.scanForAdapters(AdapterType.BLE)

        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is UnsupportedOperationException)
    }

    @Test
    fun `connect - bluetooth - success - updates state`() = runTest {
        // Given
        val adapterInfo = AdapterInfo("1", "OBD2", AdapterType.BLUETOOTH, "00:11:22:33:44:55")
        coEvery { bluetoothAdapter.connect(adapterInfo) } returns Result.Success(Unit)
        coEvery { bluetoothAdapter.isConnected() } returns true

        // When
        val result = connectionManager.connect(adapterInfo)

        // Then
        assertTrue(result is Result.Success)
        assertTrue(connectionManager.isConnected())
        assertEquals(adapterInfo, connectionManager.getCurrentAdapterInfo())
        coVerify { bluetoothAdapter.connect(adapterInfo) }
    }

    @Test
    fun `connect - usb - success - updates state`() = runTest {
        // Given
        val adapterInfo = AdapterInfo("2", "USB", AdapterType.USB, "/dev/ttyUSB0")
        coEvery { usbSerialAdapter.connect(adapterInfo) } returns Result.Success(Unit)
        coEvery { usbSerialAdapter.isConnected() } returns true

        // When
        val result = connectionManager.connect(adapterInfo)

        // Then
        assertTrue(result is Result.Success)
        assertTrue(connectionManager.isConnected())
        assertEquals(adapterInfo, connectionManager.getCurrentAdapterInfo())
        coVerify { usbSerialAdapter.connect(adapterInfo) }
    }

    @Test
    fun `connect - failure - clears state`() = runTest {
        // Given
        val adapterInfo = AdapterInfo("1", "OBD2", AdapterType.BLUETOOTH, "00:11:22:33:44:55")
        coEvery { bluetoothAdapter.connect(adapterInfo) } returns
            Result.Error(Exception("Connection failed"))

        // When
        val result = connectionManager.connect(adapterInfo)

        // Then
        assertTrue(result is Result.Error)
        assertFalse(connectionManager.isConnected())
        assertNull(connectionManager.getCurrentAdapterInfo())
    }

    @Test
    fun `disconnect - clears current adapter`() = runTest {
        // Given
        val adapterInfo = AdapterInfo("1", "OBD2", AdapterType.BLUETOOTH, "00:11:22:33:44:55")
        coEvery { bluetoothAdapter.connect(adapterInfo) } returns Result.Success(Unit)
        coEvery { bluetoothAdapter.isConnected() } returns true
        connectionManager.connect(adapterInfo)

        // When
        connectionManager.disconnect()

        // Then
        assertFalse(connectionManager.isConnected())
        assertNull(connectionManager.getCurrentAdapterInfo())
        coVerify { bluetoothAdapter.disconnect() }
    }

    @Test
    fun `sendCommand - not connected - returns error`() = runTest {
        // When
        val result = connectionManager.sendCommand("ATZ")

        // Then
        assertTrue(result is Result.Error)
        assertEquals("Not connected to any adapter", (result as Result.Error).exception.message)
    }

    @Test
    fun `sendCommand - connected - delegates to adapter`() = runTest {
        // Given
        val adapterInfo = AdapterInfo("1", "OBD2", AdapterType.BLUETOOTH, "00:11:22:33:44:55")
        coEvery { bluetoothAdapter.connect(adapterInfo) } returns Result.Success(Unit)
        coEvery { bluetoothAdapter.isConnected() } returns true
        coEvery { bluetoothAdapter.sendCommand("ATZ", any()) } returns Result.Success("OK>")
        connectionManager.connect(adapterInfo)

        // When
        val result = connectionManager.sendCommand("ATZ")

        // Then
        assertTrue(result is Result.Success)
        assertEquals("OK>", (result as Result.Success).data)
        coVerify { bluetoothAdapter.sendCommand("ATZ", any()) }
    }
}
