package com.cardiag.pro.data.logging

import android.content.Context
import com.cardiag.pro.data.model.LogEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles file-based logging with rotation and cleanup.
 */
@Singleton
class FileLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val logDir = File(context.getExternalFilesDir(null), "logs")
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    private val currentLogFile: File
        get() {
            val fileName = "cardiag_${dateFormat.format(Date())}.log"
            return File(logDir, fileName)
        }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        logDir.mkdirs()
        rotateLogsIfNeeded()
        Timber.d("FileLogger initialized, log directory: ${logDir.absolutePath}")
    }

    /**
     * Write a log entry to file asynchronously.
     */
    fun log(entry: LogEntry) {
        scope.launch {
            try {
                val logLine = entry.toLogLine()
                currentLogFile.appendText(logLine + "\n")

                // Check if rotation is needed after writing
                if (currentLogFile.length() > MAX_FILE_SIZE) {
                    rotateLogsIfNeeded()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to write log to file")
            }
        }
    }

    /**
     * Get all log files sorted by date (newest first).
     */
    fun getLogFiles(): List<File> {
        return logDir.listFiles()
            ?.filter { it.name.startsWith("cardiag_") && it.extension == "log" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Get total size of all log files.
     */
    fun getTotalLogSize(): Long {
        return getLogFiles().sumOf { it.length() }
    }

    /**
     * Clear all log files.
     */
    fun clearAllLogs() {
        scope.launch {
            try {
                getLogFiles().forEach { it.delete() }
                Timber.i("All log files cleared")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear log files")
            }
        }
    }

    /**
     * Rotate logs by deleting old files if we exceed the maximum count.
     */
    private fun rotateLogsIfNeeded() {
        try {
            val logFiles = getLogFiles()

            // Delete files beyond the maximum count
            if (logFiles.size > MAX_LOG_FILES) {
                logFiles.drop(MAX_LOG_FILES).forEach { file ->
                    file.delete()
                    Timber.d("Deleted old log file: ${file.name}")
                }
            }

            // Also delete any file that's too large
            logFiles.forEach { file ->
                if (file.length() > MAX_FILE_SIZE) {
                    file.delete()
                    Timber.d("Deleted oversized log file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to rotate logs")
        }
    }

    /**
     * Export all logs to a single file for sharing.
     */
    suspend fun exportLogs(): File? {
        return try {
            val exportFile = File(context.cacheDir, "cardiag_logs_export.txt")
            exportFile.bufferedWriter().use { writer ->
                writer.write("=== CarDiag Pro Logs Export ===\n")
                writer.write("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
                writer.write("Total log files: ${getLogFiles().size}\n\n")

                getLogFiles().forEach { logFile ->
                    writer.write("\n=== ${logFile.name} ===\n")
                    logFile.forEachLine { line ->
                        writer.write(line + "\n")
                    }
                }
            }
            Timber.i("Logs exported to: ${exportFile.absolutePath}")
            exportFile
        } catch (e: Exception) {
            Timber.e(e, "Failed to export logs")
            null
        }
    }

    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB
        private const val MAX_LOG_FILES = 5 // Keep last 5 files
    }
}
