package com.cardiag.pro.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardiag.pro.data.model.DiagnosticSession
import com.cardiag.pro.data.model.DtcCode
import com.cardiag.pro.data.repository.DiagnosticRepository
import com.cardiag.pro.utils.ReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for reports generation and management
 */
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportGenerator: ReportGenerator,
    private val diagnosticRepository: DiagnosticRepository
) : ViewModel() {

    private val _diagnosticSessions = MutableStateFlow<List<DiagnosticSession>>(emptyList())
    val diagnosticSessions: StateFlow<List<DiagnosticSession>> = _diagnosticSessions.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generatedFile = MutableStateFlow<File?>(null)
    val generatedFile: StateFlow<File?> = _generatedFile.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadDiagnosticSessions()
    }

    /**
     * Load all diagnostic sessions from database
     */
    private fun loadDiagnosticSessions() {
        viewModelScope.launch {
            try {
                diagnosticRepository.getAllDiagnosticSessions().collect { sessions ->
                    _diagnosticSessions.value = sessions.sortedByDescending { it.timestamp }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load diagnostic sessions")
                _errorMessage.value = "Failed to load diagnostic history"
            }
        }
    }

    /**
     * Generate PDF report for a diagnostic session
     */
    fun generatePdfReport(sessionId: Long) {
        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            _generatedFile.value = null

            try {
                val session = withContext(Dispatchers.IO) {
                    diagnosticRepository.getDiagnosticSessionById(sessionId)
                }

                if (session == null) {
                    _errorMessage.value = "Diagnostic session not found"
                    return@launch
                }

                val dtcCodes = withContext(Dispatchers.IO) {
                    diagnosticRepository.getDtcCodesForSession(sessionId)
                }

                val file = withContext(Dispatchers.IO) {
                    reportGenerator.generatePdfReport(session, dtcCodes)
                }

                if (file != null) {
                    _generatedFile.value = file
                    Timber.i("PDF report generated: ${file.name}")
                } else {
                    _errorMessage.value = "Failed to generate PDF report"
                }
            } catch (e: Exception) {
                Timber.e(e, "Error generating PDF report")
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Generate CSV export for a diagnostic session
     */
    fun generateCsvExport(sessionId: Long) {
        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            _generatedFile.value = null

            try {
                val session = withContext(Dispatchers.IO) {
                    diagnosticRepository.getDiagnosticSessionById(sessionId)
                }

                if (session == null) {
                    _errorMessage.value = "Diagnostic session not found"
                    return@launch
                }

                val dtcCodes = withContext(Dispatchers.IO) {
                    diagnosticRepository.getDtcCodesForSession(sessionId)
                }

                val file = withContext(Dispatchers.IO) {
                    reportGenerator.generateCsvExport(session, dtcCodes)
                }

                if (file != null) {
                    _generatedFile.value = file
                    Timber.i("CSV export generated: ${file.name}")
                } else {
                    _errorMessage.value = "Failed to generate CSV export"
                }
            } catch (e: Exception) {
                Timber.e(e, "Error generating CSV export")
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear generated file reference
     */
    fun clearGeneratedFile() {
        _generatedFile.value = null
    }
}
