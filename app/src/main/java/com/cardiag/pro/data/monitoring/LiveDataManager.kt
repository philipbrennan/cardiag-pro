package com.cardiag.pro.data.monitoring

import com.cardiag.pro.data.connection.ELM327Protocol
import com.cardiag.pro.data.model.LiveSensorData
import com.cardiag.pro.data.model.ObdPid
import com.cardiag.pro.data.parser.PidParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages live data monitoring from OBD2 adapter.
 * Polls PIDs at configurable intervals and emits updates.
 */
@Singleton
class LiveDataManager @Inject constructor(
    private val elm327Protocol: ELM327Protocol
) {
    private val _liveData = MutableStateFlow(LiveSensorData())
    val liveData: StateFlow<LiveSensorData> = _liveData.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var monitoringJob: Job? = null
    private var refreshRateMs: Long = 1000 // Default 1Hz

    /**
     * Start monitoring live data.
     * Polls configured PIDs at the specified refresh rate.
     */
    fun startMonitoring(
        pids: List<ObdPid> = ObdPid.getCommonPids(),
        refreshRateHz: Double = 1.0,
        scope: CoroutineScope
    ) {
        if (_isMonitoring.value) {
            Timber.w("Monitoring already active")
            return
        }

        refreshRateMs = (1000 / refreshRateHz).toLong()
        Timber.i("Starting live data monitoring at ${refreshRateHz}Hz (${refreshRateMs}ms interval)")

        _isMonitoring.value = true

        monitoringJob = scope.launch {
            while (isActive && _isMonitoring.value) {
                try {
                    // Request each PID individually for reliability
                    val pidResults = mutableMapOf<ObdPid, Any>()
                    
                    for (pid in pids) {
                        if (!isActive) break
                        
                        val command = pid.getCommand()
                        val response = elm327Protocol.sendCommand(command, timeoutMs = 500)
                        
                        if (response != null && !response.contains("NO DATA", ignoreCase = true)) {
                            val parsed = PidParser.parseResponse(response)
                            pidResults.putAll(parsed)
                        }
                    }

                    // Update live data
                    if (pidResults.isNotEmpty()) {
                        _liveData.value = PidParser.toLiveSensorData(pidResults)
                    }

                    // Wait for next refresh interval
                    delay(refreshRateMs)
                    
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Error during live data polling")
                    delay(refreshRateMs)
                }
            }
        }
    }

    /**
     * Start monitoring with optimized multi-PID requests.
     * Some adapters support requesting multiple PIDs in one command.
     */
    fun startMonitoringOptimized(
        pids: List<ObdPid> = ObdPid.getCommonPids(),
        refreshRateHz: Double = 1.0,
        scope: CoroutineScope
    ) {
        if (_isMonitoring.value) {
            Timber.w("Monitoring already active")
            return
        }

        refreshRateMs = (1000 / refreshRateHz).toLong()
        Timber.i("Starting optimized live data monitoring at ${refreshRateHz}Hz")

        _isMonitoring.value = true

        monitoringJob = scope.launch {
            while (isActive && _isMonitoring.value) {
                try {
                    // Split PIDs into smaller batches (max 3-4 PIDs per request for reliability)
                    val batches = pids.chunked(3)
                    val allResults = mutableMapOf<ObdPid, Any>()
                    
                    for (batch in batches) {
                        if (!isActive) break
                        
                        val command = PidParser.buildMultiPidRequest(batch)
                        val response = elm327Protocol.sendCommand(command, timeoutMs = 800)
                        
                        if (response != null && !response.contains("NO DATA", ignoreCase = true)) {
                            val parsed = PidParser.parseResponse(response)
                            allResults.putAll(parsed)
                        }
                    }

                    // Update live data
                    if (allResults.isNotEmpty()) {
                        _liveData.value = PidParser.toLiveSensorData(allResults)
                    }

                    // Wait for next refresh interval
                    delay(refreshRateMs)
                    
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Error during optimized live data polling")
                    delay(refreshRateMs)
                }
            }
        }
    }

    /**
     * Stop monitoring live data.
     */
    fun stopMonitoring() {
        Timber.i("Stopping live data monitoring")
        _isMonitoring.value = false
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * Update refresh rate while monitoring.
     */
    fun setRefreshRate(refreshRateHz: Double) {
        refreshRateMs = (1000 / refreshRateHz).toLong()
        Timber.d("Refresh rate updated to ${refreshRateHz}Hz (${refreshRateMs}ms)")
    }

    /**
     * Read a single PID value (one-shot, not monitoring).
     */
    suspend fun readPid(pid: ObdPid): Any? {
        return try {
            val command = pid.getCommand()
            val response = elm327Protocol.sendCommand(command, timeoutMs = 1000)
            
            if (response != null && !response.contains("NO DATA", ignoreCase = true)) {
                val parsed = PidParser.parseResponse(response)
                parsed[pid]
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read PID ${pid.displayName}")
            null
        }
    }
}
