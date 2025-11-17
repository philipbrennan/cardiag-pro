package com.cardiag.pro.data.logging

import android.util.Log
import com.cardiag.pro.data.model.LogEntry
import com.cardiag.pro.data.model.LogLevel
import timber.log.Timber

/**
 * Custom Timber Tree that writes logs to file and memory.
 */
class FileLoggingTree(
    private val fileLogger: FileLogger,
    private val logRepository: LogRepository
) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Convert Android log priority to our LogLevel
        val logLevel = when (priority) {
            Log.VERBOSE, Log.DEBUG -> LogLevel.DEBUG
            Log.INFO -> LogLevel.INFO
            Log.WARN -> LogLevel.WARN
            Log.ERROR, Log.ASSERT -> LogLevel.ERROR
            else -> LogLevel.DEBUG
        }

        // Create log entry
        val entry = LogEntry(
            level = logLevel,
            tag = tag ?: "Unknown",
            message = message,
            throwable = t
        )

        // Write to file
        fileLogger.log(entry)

        // Add to in-memory repository for live viewing
        logRepository.addLog(entry)
    }
}
