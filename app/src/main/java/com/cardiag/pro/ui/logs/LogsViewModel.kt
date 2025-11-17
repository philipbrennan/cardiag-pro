package com.cardiag.pro.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardiag.pro.data.logging.LogRepository
import com.cardiag.pro.data.model.LogEntry
import com.cardiag.pro.data.model.LogLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for logs screen.
 */
@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logRepository: LogRepository
) : ViewModel() {

    // Current filter level
    private val _filterLevel = MutableStateFlow<LogLevel?>(null)
    val filterLevel: StateFlow<LogLevel?> = _filterLevel.asStateFlow()

    // Filtered logs
    private val _filteredLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val filteredLogs: StateFlow<List<LogEntry>> = _filteredLogs.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow<LogsUiState>(LogsUiState.Idle)
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    init {
        observeLogs()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            logRepository.logs.collect { allLogs ->
                applyFilter(allLogs)
            }
        }
    }

    /**
     * Set filter level.
     */
    fun setFilter(level: LogLevel?) {
        _filterLevel.value = level
        val allLogs = logRepository.logs.value
        applyFilter(allLogs)
    }

    private fun applyFilter(allLogs: List<LogEntry>) {
        val filtered = if (_filterLevel.value == null) {
            allLogs
        } else {
            allLogs.filter { it.level.priority >= _filterLevel.value!!.priority }
        }
        _filteredLogs.value = filtered
    }

    /**
     * Clear all logs.
     */
    fun clearLogs() {
        viewModelScope.launch {
            logRepository.clearAllLogs()
            _uiState.value = LogsUiState.LogsCleared
            Timber.i("Logs cleared by user")
        }
    }

    /**
     * Export logs to file.
     */
    fun exportLogs() {
        viewModelScope.launch {
            _uiState.value = LogsUiState.Exporting

            val exportFile = logRepository.getFileLogger().exportLogs()

            if (exportFile != null) {
                _uiState.value = LogsUiState.ExportSuccess(exportFile.absolutePath)
                Timber.i("Logs exported successfully to: ${exportFile.absolutePath}")
            } else {
                _uiState.value = LogsUiState.ExportError("Failed to export logs")
                Timber.e("Failed to export logs")
            }
        }
    }

    /**
     * Get log statistics.
     */
    fun getLogStats(): LogStats {
        val allLogs = logRepository.logs.value
        return LogStats(
            total = allLogs.size,
            debug = allLogs.count { it.level == LogLevel.DEBUG },
            info = allLogs.count { it.level == LogLevel.INFO },
            warn = allLogs.count { it.level == LogLevel.WARN },
            error = allLogs.count { it.level == LogLevel.ERROR }
        )
    }

    /**
     * Reset UI state.
     */
    fun resetState() {
        _uiState.value = LogsUiState.Idle
    }
}

/**
 * UI states for logs screen.
 */
sealed class LogsUiState {
    object Idle : LogsUiState()
    object Exporting : LogsUiState()
    data class ExportSuccess(val filePath: String) : LogsUiState()
    data class ExportError(val message: String) : LogsUiState()
    object LogsCleared : LogsUiState()
}

/**
 * Log statistics.
 */
data class LogStats(
    val total: Int,
    val debug: Int,
    val info: Int,
    val warn: Int,
    val error: Int
)
