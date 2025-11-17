package com.cardiag.pro.ui.diagnostic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardiag.pro.data.model.DiagnosticTroubleCode
import com.cardiag.pro.data.model.ECUSystem
import com.cardiag.pro.data.model.Manufacturer
import com.cardiag.pro.data.model.Result
import com.cardiag.pro.data.model.VehicleInfo
import com.cardiag.pro.data.repository.DtcRepository
import com.cardiag.pro.data.repository.VinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for diagnostic operations.
 */
@HiltViewModel
class DiagnosticViewModel @Inject constructor(
    private val vinRepository: VinRepository,
    private val dtcRepository: DtcRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiagnosticUiState>(DiagnosticUiState.Idle)
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    private val _vehicleInfo = MutableStateFlow<VehicleInfo?>(null)
    val vehicleInfo: StateFlow<VehicleInfo?> = _vehicleInfo.asStateFlow()

    private val _dtcCodes = MutableStateFlow<List<DiagnosticTroubleCode>>(emptyList())
    val dtcCodes: StateFlow<List<DiagnosticTroubleCode>> = _dtcCodes.asStateFlow()

    private val _freezeFrames = MutableStateFlow<Map<String, com.cardiag.pro.data.model.FreezeFrameData>>(emptyMap())
    val freezeFrames: StateFlow<Map<String, com.cardiag.pro.data.model.FreezeFrameData>> = _freezeFrames.asStateFlow()

    /**
     * Read VIN from vehicle.
     */
    fun readVin() {
        viewModelScope.launch {
            _uiState.value = DiagnosticUiState.ReadingVin

            when (val result = vinRepository.readVin()) {
                is Result.Success -> {
                    _vehicleInfo.value = result.data
                    _uiState.value = DiagnosticUiState.VinReadSuccess(result.data)
                    Timber.i("VIN read successfully: ${result.data.vin}")
                }
                is Result.Error -> {
                    _uiState.value = DiagnosticUiState.Error(
                        result.exception.message ?: "Failed to read VIN"
                    )
                    Timber.e(result.exception, "Failed to read VIN")
                }
            }
        }
    }

    /**
     * Set vehicle info manually (fallback when VIN reading fails).
     */
    fun setManualVehicleInfo(manufacturer: Manufacturer, vin: String? = null) {
        val vehicleInfo = vinRepository.createManualVehicleInfo(
            vin = vin,
            manufacturer = manufacturer
        )
        _vehicleInfo.value = vehicleInfo
        _uiState.value = DiagnosticUiState.VinReadSuccess(vehicleInfo)
        Timber.i("Manual vehicle info set: $manufacturer")
    }

    /**
     * Read DTC codes from vehicle.
     */
    fun readDtcCodes(system: ECUSystem = ECUSystem.ENGINE) {
        viewModelScope.launch {
            _uiState.value = DiagnosticUiState.ReadingCodes

            when (val result = dtcRepository.readDtcCodes(system)) {
                is Result.Success -> {
                    _dtcCodes.value = result.data

                    if (result.data.isEmpty()) {
                        _uiState.value = DiagnosticUiState.NoCodesFound
                    } else {
                        _uiState.value = DiagnosticUiState.CodesReadSuccess(result.data.size)
                    }

                    Timber.i("Read ${result.data.size} DTC codes")
                }
                is Result.Error -> {
                    _uiState.value = DiagnosticUiState.Error(
                        result.exception.message ?: "Failed to read codes"
                    )
                    Timber.e(result.exception, "Failed to read DTC codes")
                }
            }
        }
    }

    /**
     * Read DTC codes from all available ECUs.
     */
    fun readDtcCodesFromAllECUs() {
        viewModelScope.launch {
            _uiState.value = DiagnosticUiState.ReadingCodes

            val manufacturer = _vehicleInfo.value?.manufacturer

            when (val result = dtcRepository.readDtcCodesFromAllECUs(manufacturer)) {
                is Result.Success -> {
                    // Flatten all codes from all ECUs into a single list
                    val allCodes = result.data.values.flatten()
                    _dtcCodes.value = allCodes

                    if (allCodes.isEmpty()) {
                        _uiState.value = DiagnosticUiState.NoCodesFound
                        _freezeFrames.value = emptyMap()
                    } else {
                        _uiState.value = DiagnosticUiState.CodesReadSuccess(allCodes.size)
                        
                        // Automatically read freeze frames for all codes
                        Timber.i("Reading freeze frames for ${allCodes.size} codes...")
                        val freezeFrameData = dtcRepository.readAllFreezeFrames(allCodes)
                        _freezeFrames.value = freezeFrameData
                        Timber.i("Retrieved ${freezeFrameData.size} freeze frames")
                    }

                    Timber.i("Read ${allCodes.size} DTC codes from ${result.data.size} ECUs")
                }
                is Result.Error -> {
                    _uiState.value = DiagnosticUiState.Error(
                        result.exception.message ?: "Failed to read codes from ECUs"
                    )
                    Timber.e(result.exception, "Failed to read DTC codes from all ECUs")
                }
            }
        }
    }

    /**
     * Clear DTC codes from vehicle.
     */
    fun clearDtcCodes() {
        viewModelScope.launch {
            _uiState.value = DiagnosticUiState.ClearingCodes

            when (val result = dtcRepository.clearDtcCodes()) {
                is Result.Success -> {
                    _dtcCodes.value = emptyList()
                    _uiState.value = DiagnosticUiState.CodesCleared
                    Timber.i("DTC codes cleared successfully")
                }
                is Result.Error -> {
                    _uiState.value = DiagnosticUiState.Error(
                        result.exception.message ?: "Failed to clear codes"
                    )
                    Timber.e(result.exception, "Failed to clear DTC codes")
                }
            }
        }
    }

    /**
     * Save current diagnostic session to database.
     */
    fun saveDiagnosticSession(notes: String? = null) {
        viewModelScope.launch {
            val vehicle = _vehicleInfo.value
            val codes = _dtcCodes.value
            val freezeFrameData = _freezeFrames.value

            if (codes.isEmpty()) {
                _uiState.value = DiagnosticUiState.Error("No codes to save")
                return@launch
            }

            // Determine which systems were scanned
            val systemsScanned = codes.map { it.system.displayName }.distinct().joinToString(", ")

            when (val result = dtcRepository.saveDiagnosticSession(
                vin = vehicle?.vin,
                manufacturer = vehicle?.manufacturer,
                codes = codes,
                freezeFrames = freezeFrameData,
                notes = notes,
                systemScanned = systemsScanned
            )) {
                is Result.Success -> {
                    _uiState.value = DiagnosticUiState.SessionSaved
                    Timber.i("Diagnostic session saved: ${result.data} (${freezeFrameData.size} freeze frames)")
                }
                is Result.Error -> {
                    _uiState.value = DiagnosticUiState.Error(
                        result.exception.message ?: "Failed to save session"
                    )
                    Timber.e(result.exception, "Failed to save diagnostic session")
                }
            }
        }
    }

    /**
     * Reset UI state to idle.
     */
    fun resetState() {
        _uiState.value = DiagnosticUiState.Idle
    }
}

/**
 * UI states for diagnostic screen.
 */
sealed class DiagnosticUiState {
    object Idle : DiagnosticUiState()
    object ReadingVin : DiagnosticUiState()
    data class VinReadSuccess(val vehicleInfo: VehicleInfo) : DiagnosticUiState()
    object ReadingCodes : DiagnosticUiState()
    data class CodesReadSuccess(val count: Int) : DiagnosticUiState()
    object NoCodesFound : DiagnosticUiState()
    object ClearingCodes : DiagnosticUiState()
    object CodesCleared : DiagnosticUiState()
    object SessionSaved : DiagnosticUiState()
    data class Error(val message: String) : DiagnosticUiState()
}
