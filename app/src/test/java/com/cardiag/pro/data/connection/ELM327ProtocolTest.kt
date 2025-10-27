package com.cardiag.pro.data.connection

import com.cardiag.pro.data.model.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ELM327ProtocolTest {

    private lateinit var connectionManager: ConnectionManager
    private lateinit var elm327Protocol: ELM327Protocol

    @BeforeEach
    fun setup() {
        connectionManager = mockk(relaxed = true)
        elm327Protocol = ELM327Protocol(connectionManager)
    }

    @Test
    fun `initialize - success - returns success result`() = runTest {
        // Given
        coEvery { connectionManager.isConnected() } returns true
        coEvery { connectionManager.sendCommand(any(), any()) } returns Result.Success("OK>")

        // When
        val result = elm327Protocol.initialize()

        // Then
        assertTrue(result is Result.Success)
        assertTrue(elm327Protocol.isInitialized())

        // Verify initialization commands were sent
        coVerify { connectionManager.sendCommand("ATZ", any()) }
        coVerify { connectionManager.sendCommand("ATE0", any()) }
        coVerify { connectionManager.sendCommand("ATL0", any()) }
        coVerify { connectionManager.sendCommand("ATS0", any()) }
        coVerify { connectionManager.sendCommand("ATSP0", any()) }
        coVerify { connectionManager.sendCommand("0100", any()) }
    }

    @Test
    fun `initialize - not connected - returns error`() = runTest {
        // Given
        coEvery { connectionManager.isConnected() } returns false

        // When
        val result = elm327Protocol.initialize()

        // Then
        assertTrue(result is Result.Error)
        assertFalse(elm327Protocol.isInitialized())
        assertEquals("Not connected to adapter", (result as Result.Error).exception.message)
    }

    @Test
    fun `initialize - command fails - returns error`() = runTest {
        // Given
        coEvery { connectionManager.isConnected() } returns true
        coEvery { connectionManager.sendCommand("ATZ", any()) } returns Result.Success("OK>")
        coEvery { connectionManager.sendCommand("ATE0", any()) } returns Result.Success("OK>")
        coEvery { connectionManager.sendCommand("ATL0", any()) } returns Result.Success("OK>")
        coEvery { connectionManager.sendCommand("ATS0", any()) } returns Result.Success("OK>")
        coEvery { connectionManager.sendCommand("ATSP0", any()) } returns
            Result.Error(Exception("Protocol error"))

        // When
        val result = elm327Protocol.initialize()

        // Then
        assertTrue(result is Result.Error)
        assertFalse(elm327Protocol.isInitialized())
    }

    @Test
    fun `sendCommand - success - returns cleaned response`() = runTest {
        // Given
        val rawResponse = "41 00 BE 3F A8 13>"
        coEvery { connectionManager.sendCommand("0100", any()) } returns Result.Success(rawResponse)

        // When
        val response = elm327Protocol.sendCommand("0100")

        // Then
        assertNotNull(response)
        assertFalse(response!!.contains(">"))
        assertFalse(response.contains("\r"))
        assertFalse(response.contains("\n"))
    }

    @Test
    fun `sendCommand - failure - returns null`() = runTest {
        // Given
        coEvery { connectionManager.sendCommand("0100", any()) } returns
            Result.Error(Exception("Timeout"))

        // When
        val response = elm327Protocol.sendCommand("0100")

        // Then
        assertNull(response)
    }

    @Test
    fun `resetInitialization - resets initialized state`() {
        // Given
        elm327Protocol.resetInitialization()

        // When
        val isInitialized = elm327Protocol.isInitialized()

        // Then
        assertFalse(isInitialized)
    }
}
