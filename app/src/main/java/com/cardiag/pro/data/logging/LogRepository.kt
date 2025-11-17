package com.cardiag.pro.data.logging

import com.cardiag.pro.data.model.LogEntry
import com.cardiag.pro.data.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing log entries in memory and file.
 * Provides real-time log streaming for UI.
 */
@Singleton
class LogRepository @Inject constructor(
    private val fileLogger: FileLogger
) {

    // In-memory log buffer (circular buffer with max size)
    private val logBuffer = ConcurrentLinkedQueue<LogEntry>()

    // Live log stream for UI
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    init {
        Timber.d("LogRepository initialized")
    }

    /**
     * Add a log entry to memory and file.
     */
    fun addLog(entry: LogEntry) {
        // Add to memory buffer
        logBuffer.add(entry)

        // Enforce max buffer size
        while (logBuffer.size > MAX_BUFFER_SIZE) {
            logBuffer.poll()
        }

        // Update flow for UI
        _logs.value = logBuffer.toList()

        // Write to file
        fileLogger.log(entry)
    }

    /**
     * Convenience method to log with just message and level.
     */
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, String> = emptyMap()) {
        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
            metadata = metadata
        )
        addLog(entry)
    }

    /**
     * Log debug message.
     */
    fun debug(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.DEBUG, tag, message, metadata = metadata)
    }

    /**
     * Log info message.
     */
    fun info(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.INFO, tag, message, metadata = metadata)
    }

    /**
     * Log warning message.
     */
    fun warn(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.WARN, tag, message, metadata = metadata)
    }

    /**
     * Log error message.
     */
    fun error(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.ERROR, tag, message, throwable, metadata)
    }

    /**
     * Get logs filtered by level.
     */
    fun getFilteredLogs(minLevel: LogLevel): List<LogEntry> {
        return logBuffer.filter { it.level.priority >= minLevel.priority }
    }

    /**
     * Get logs filtered by tag.
     */
    fun getLogsForTag(tag: String): List<LogEntry> {
        return logBuffer.filter { it.tag == tag }
    }

    /**
     * Search logs by message content.
     */
    fun searchLogs(query: String): List<LogEntry> {
        return logBuffer.filter {
            it.message.contains(query, ignoreCase = true) ||
            it.tag.contains(query, ignoreCase = true)
        }
    }

    /**
     * Clear all in-memory logs.
     */
    fun clearMemoryLogs() {
        logBuffer.clear()
        _logs.value = emptyList()
        Timber.i("Memory logs cleared")
    }

    /**
     * Clear all logs (memory and files).
     */
    fun clearAllLogs() {
        clearMemoryLogs()
        fileLogger.clearAllLogs()
        Timber.i("All logs cleared")
    }

    /**
     * Get file logger for direct access.
     */
    fun getFileLogger(): FileLogger = fileLogger

    companion object {
        private const val MAX_BUFFER_SIZE = 1000 // Keep last 1000 entries in memory
    }
}
