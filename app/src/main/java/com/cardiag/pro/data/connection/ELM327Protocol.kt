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
            Timber.d("Initializing ELM327 adapter")

            if (!connectionManager.isConnected()) {
                return Result.Error(IllegalStateException("Not connected to adapter"))
            }

            // Reset adapter
            sendCommand("ATZ")?.let { delay(1000) } // Wait for reset

            // Turn off echo
            sendCommand("ATE0")

            // Turn off line feed
            sendCommand("ATL0")

            // Turn off spaces
            sendCommand("ATS0")

            // Set automatic protocol detection
            val protocolResult = sendCommand("ATSP0")
            if (protocolResult == null) {
                return Result.Error(IOException("Failed to set protocol"))
            }

            // Test communication
            val testResult = sendCommand("0100")
            if (testResult == null) {
                return Result.Error(IOException("Failed to communicate with vehicle"))
            }

            isInitialized = true
            Timber.i("ELM327 adapter initialized successfully")
            return Result.Success("Initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize ELM327 adapter")
            isInitialized = false
            return Result.Error(e as? Exception ?: Exception(e))
        }
    }

    /**
     * Send a command and return the response.
     * Returns null if command fails.
     */
    suspend fun sendCommand(command: String, timeoutMs: Long = 5000): String? {
        val result = connectionManager.sendCommand(command, timeoutMs)
        return when (result) {
            is Result.Success -> {
                // Remove echo and prompt from response
                cleanResponse(result.data)
            }
            is Result.Error -> {
                Timber.e(result.exception, "Command failed: $command")
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
