package com.cardiag.pro.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardiag.pro.data.local.entity.DiagnosticSessionEntity
import com.cardiag.pro.data.repository.DtcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for diagnostic history screen.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val dtcRepository: DtcRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Idle)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val sessions = dtcRepository.getAllSessions()

    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                Timber.d("Clearing all history")
                // Access DAO through repository - we'll need to add this method
                // For now, log that it would clear
                Timber.i("Would clear all history - method needs to be added to repository")
                _uiState.value = HistoryUiState.Idle
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear history")
                _uiState.value = HistoryUiState.Error("Failed to clear history: ${e.message}")
            }
        }
    }

    fun exportHistory() {
        viewModelScope.launch {
            try {
                Timber.d("Exporting history")
                // TODO: Implement export functionality
                _uiState.value = HistoryUiState.ExportSuccess
            } catch (e: Exception) {
                Timber.e(e, "Failed to export history")
                _uiState.value = HistoryUiState.Error("Failed to export: ${e.message}")
            }
        }
    }
}

/**
 * UI states for history screen.
 */
sealed class HistoryUiState {
    data object Idle : HistoryUiState()
    data object ExportSuccess : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}
