package com.cardiag.pro.ui.monitoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardiag.pro.data.model.LiveSensorData
import com.cardiag.pro.data.model.ObdPid
import com.cardiag.pro.data.monitoring.LiveDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for live data monitoring screen.
 */
@HiltViewModel
class MonitoringViewModel @Inject constructor(
    private val liveDataManager: LiveDataManager
) : ViewModel() {

    val liveData: StateFlow<LiveSensorData> = liveDataManager.liveData
    val isMonitoring: StateFlow<Boolean> = liveDataManager.isMonitoring

    private val _refreshRate = MutableStateFlow(1.0) // Hz
    val refreshRate: StateFlow<Double> = _refreshRate.asStateFlow()

    private val _useMetric = MutableStateFlow(true)
    val useMetric: StateFlow<Boolean> = _useMetric.asStateFlow()

    private val _selectedPids = MutableStateFlow(ObdPid.getCommonPids())
    val selectedPids: StateFlow<List<ObdPid>> = _selectedPids.asStateFlow()

    /**
     * Start monitoring with current settings.
     */
    fun startMonitoring() {
        Timber.i("Starting monitoring from ViewModel")
        liveDataManager.startMonitoring(
            pids = _selectedPids.value,
            refreshRateHz = _refreshRate.value,
            scope = viewModelScope
        )
    }

    /**
     * Start monitoring with optimized multi-PID requests.
     */
    fun startMonitoringOptimized() {
        Timber.i("Starting optimized monitoring from ViewModel")
        liveDataManager.startMonitoringOptimized(
            pids = _selectedPids.value,
            refreshRateHz = _refreshRate.value,
            scope = viewModelScope
        )
    }

    /**
     * Stop monitoring.
     */
    fun stopMonitoring() {
        Timber.i("Stopping monitoring from ViewModel")
        liveDataManager.stopMonitoring()
    }

    /**
     * Toggle monitoring on/off.
     */
    fun toggleMonitoring() {
        if (isMonitoring.value) {
            stopMonitoring()
        } else {
            startMonitoring()
        }
    }

    /**
     * Update refresh rate.
     */
    fun setRefreshRate(rateHz: Double) {
        _refreshRate.value = rateHz.coerceIn(0.5, 5.0) // Limit to 0.5-5 Hz
        if (isMonitoring.value) {
            liveDataManager.setRefreshRate(_refreshRate.value)
        }
        Timber.d("Refresh rate set to ${_refreshRate.value}Hz")
    }

    /**
     * Toggle metric/imperial units.
     */
    fun toggleUnits() {
        _useMetric.value = !_useMetric.value
        Timber.d("Units toggled to ${if (_useMetric.value) "metric" else "imperial"}")
    }

    /**
     * Update selected PIDs for monitoring.
     */
    fun setSelectedPids(pids: List<ObdPid>) {
        _selectedPids.value = pids
        Timber.d("Selected PIDs updated: ${pids.size} PIDs")
    }

    /**
     * Convert speed to appropriate unit.
     */
    fun formatSpeed(kmh: Int?): String {
        if (kmh == null) return "--"
        return if (_useMetric.value) {
            "$kmh km/h"
        } else {
            "${(kmh * 0.621371).toInt()} mph"
        }
    }

    /**
     * Convert temperature to appropriate unit.
     */
    fun formatTemperature(celsius: Int?): String {
        if (celsius == null) return "--"
        return if (_useMetric.value) {
            "$celsius°C"
        } else {
            "${(celsius * 9 / 5 + 32)}°F"
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
