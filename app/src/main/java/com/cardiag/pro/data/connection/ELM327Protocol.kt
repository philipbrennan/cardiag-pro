package com.cardiag.pro.data.connection

import com.cardiag.pro.data.model.Result
import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles ELM327 protocol communication.
 * Provides high-level methods for initialization and commands.
 */
@Singleton
class ELM327Protocol @Inject constructor(
    private val connectionManager: ConnectionManager
) {

    private var isInitialized = false

    /**
     * Initialize the ELM327 adapter with standard commands.
     */
    suspend fun initialize(): Result<String> {
        try {
            Timber.d("=== ELM327 Initialization Started ===")

            if (!connectionManager.isConnected()) {
                Timber.e("Cannot initialize: Not connected to adapter")
                return Result.Error(IllegalStateException("Not connected to adapter"))
            }

            // Reset adapter
            Timber.d("Sending ATZ (Reset)")
            val resetResponse = sendCommand("ATZ")
            Timber.i("ATZ Response: $resetResponse")
            if (resetResponse != null) {
                delay(1000) // Wait for reset
            }

            // Turn off echo
            Timber.d("Sending ATE0 (Echo Off)")
            val echoResponse = sendCommand("ATE0")
            Timber.i("ATE0 Response: $echoResponse")

            // Turn off line feed
            Timber.d("Sending ATL0 (Line Feed Off)")
            val lineFeedResponse = sendCommand("ATL0")
            Timber.i("ATL0 Response: $lineFeedResponse")

            // Turn off spaces
            Timber.d("Sending ATS0 (Spaces Off)")
            val spacesResponse = sendCommand("ATS0")
            Timber.i("ATS0 Response: $spacesResponse")

            // Set automatic protocol detection
            Timber.d("Sending ATSP0 (Auto Protocol)")
            val protocolResult = sendCommand("ATSP0")
            Timber.i("ATSP0 Response: $protocolResult")
            if (protocolResult == null) {
                Timber.e("Failed to set protocol: No response from ATSP0")
                return Result.Error(IOException("Failed to set protocol"))
            }

            // Test communication
            Timber.d("Sending 0100 (Test Communication - SAE Standard PIDs)")
            val testResult = sendCommand("0100")
            Timber.i("0100 Response: $testResult")
            if (testResult == null) {
                Timber.e("Failed to communicate with vehicle: No response from 0100")
                return Result.Error(IOException("Failed to communicate with vehicle"))
            }

            isInitialized = true
            Timber.i("=== ELM327 Initialization Complete ===")
            return Result.Success("Initialized")
        } catch (e: Exception) {
            Timber.e(e, "ELM327 initialization failed with exception")
            isInitialized = false
            return Result.Error(e as? Exception ?: Exception(e))
        }
    }

    /**
     * Send a command and return the response.
     * Returns null if command fails.
     */
    suspend fun sendCommand(command: String, timeoutMs: Long = 5000): String? {
        val startTime = System.currentTimeMillis()
        Timber.d(">>> Sending: $command (timeout: ${timeoutMs}ms)")

        val result = connectionManager.sendCommand(command, timeoutMs)
        val elapsedTime = System.currentTimeMillis() - startTime

        return when (result) {
            is Result.Success -> {
                // Remove echo and prompt from response
                val cleaned = cleanResponse(result.data)
                Timber.d("<<< Received: $cleaned (${elapsedTime}ms)")
                cleaned
            }
            is Result.Error -> {
                Timber.e(result.exception, "Command failed: $command (${elapsedTime}ms)")
                null
            }
        }
    }

    /**
     * Clean ELM327 response by removing echo, prompt, and extra whitespace.
     */
    private fun cleanResponse(response: String): String {
        return response
            .replace(">", "")           // Remove prompt
            .replace("\r", "")          // Remove carriage returns
            .replace("\n", " ")         // Replace newlines with spaces
            .trim()                     // Remove leading/trailing whitespace
            .replace(Regex("\\s+"), " ") // Collapse multiple spaces
    }

    /**
     * Check if the adapter is initialized.
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Reset initialization state.
     */
    fun resetInitialization() {
        isInitialized = false
    }
}
