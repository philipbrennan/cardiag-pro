package com.cardiag.pro.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardiag.pro.data.connection.ConnectionManager
import com.cardiag.pro.data.connection.ELM327Protocol
import com.cardiag.pro.data.model.AdapterInfo
import com.cardiag.pro.data.model.AdapterType
import com.cardiag.pro.data.model.ConnectionState
import com.cardiag.pro.data.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the connection screen.
 * Manages adapter scanning, connection, and initialization.
 */
@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val elm327Protocol: ELM327Protocol
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConnectionUiState>(ConnectionUiState.Idle)
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    private val _adapters = MutableStateFlow<List<AdapterInfo>>(emptyList())
    val adapters: StateFlow<List<AdapterInfo>> = _adapters.asStateFlow()

    private val _events = MutableSharedFlow<ConnectionEvent>()
    val events: SharedFlow<ConnectionEvent> = _events.asSharedFlow()

    // Observe connection manager state
    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState

    init {
        Timber.d("ConnectionViewModel initialized")
    }

    /**
     * Scan for Bluetooth adapters.
     */
    fun scanBluetooth() {
        viewModelScope.launch {
            _uiState.value = ConnectionUiState.Scanning
            Timber.d("Scanning for Bluetooth adapters")

            when (val result = connectionManager.scanForAdapters(AdapterType.BLUETOOTH)) {
                is Result.Success -> {
                    _adapters.value = result.data
                    _uiState.value = ConnectionUiState.AdaptersFound(result.data.size)
                    Timber.i("Found ${result.data.size} Bluetooth adapters")

                    if (result.data.isEmpty()) {
                        _events.emit(ConnectionEvent.ShowMessage("No Bluetooth adapters found"))
                    }
                }
                is Result.Error -> {
                    _uiState.value = ConnectionUiState.Error(
                        result.exception.message ?: "Failed to scan for Bluetooth adapters"
                    )
                    Timber.e(result.exception, "Bluetooth scan failed")
                    _events.emit(
                        ConnectionEvent.ShowError(
                            result.exception.message ?: "Bluetooth scan failed"
                        )
                    )
                }
            }
        }
    }

    /**
     * Scan for USB adapters.
     */
    fun scanUsb() {
        viewModelScope.launch {
            _uiState.value = ConnectionUiState.Scanning
            Timber.d("Scanning for USB adapters")

            when (val result = connectionManager.scanForAdapters(AdapterType.USB)) {
                is Result.Success -> {
                    _adapters.value = result.data
                    _uiState.value = ConnectionUiState.AdaptersFound(result.data.size)
                    Timber.i("Found ${result.data.size} USB adapters")

                    if (result.data.isEmpty()) {
                        _events.emit(ConnectionEvent.ShowMessage("No USB adapters found. Check connection and permissions."))
                    }
                }
                is Result.Error -> {
                    _uiState.value = ConnectionUiState.Error(
                        result.exception.message ?: "Failed to scan for USB adapters"
                    )
                    Timber.e(result.exception, "USB scan failed")
                    _events.emit(
                        ConnectionEvent.ShowError(
                            result.exception.message ?: "USB scan failed"
                        )
                    )
                }
            }
        }
    }

    /**
     * Connect to a specific adapter.
     */
    fun connect(adapterInfo: AdapterInfo) {
        viewModelScope.launch {
            _uiState.value = ConnectionUiState.Connecting
            Timber.d("Connecting to ${adapterInfo.name}")

            when (val result = connectionManager.connect(adapterInfo)) {
                is Result.Success -> {
                    Timber.i("Connected successfully, initializing ELM327")
                    _uiState.value = ConnectionUiState.Initializing

                    // Initialize ELM327 protocol
                    when (val initResult = elm327Protocol.initialize()) {
                        is Result.Success -> {
                            _uiState.value = ConnectionUiState.Connected(adapterInfo)
                            _events.emit(ConnectionEvent.ShowMessage("Connected to ${adapterInfo.name}"))
                            Timber.i("ELM327 initialized successfully")
                        }
                        is Result.Error -> {
                            _uiState.value = ConnectionUiState.Error(
                                "Connected but failed to initialize: ${initResult.exception.message}"
                            )
                            Timber.e(initResult.exception, "ELM327 initialization failed")
                            _events.emit(
                                ConnectionEvent.ShowError(
                                    "Failed to initialize adapter: ${initResult.exception.message}"
                                )
                            )
                            // Disconnect since initialization failed
                            connectionManager.disconnect()
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.value = ConnectionUiState.Error(
                        result.exception.message ?: "Connection failed"
                    )
                    Timber.e(result.exception, "Connection failed")
                    _events.emit(
                        ConnectionEvent.ShowError(
                            result.exception.message ?: "Connection failed"
                        )
                    )
                }
            }
        }
    }

    /**
     * Disconnect from the current adapter.
     */
    fun disconnect() {
        viewModelScope.launch {
            Timber.d("Disconnecting")
            connectionManager.disconnect()
            elm327Protocol.resetInitialization()
            _uiState.value = ConnectionUiState.Idle
            _adapters.value = emptyList()
            _events.emit(ConnectionEvent.ShowMessage("Disconnected"))
        }
    }

    /**
     * Clear the adapter list.
     */
    fun clearAdapters() {
        _adapters.value = emptyList()
        _uiState.value = ConnectionUiState.Idle
    }
}

/**
 * UI state for the connection screen.
 */
sealed class ConnectionUiState {
    object Idle : ConnectionUiState()
    object Scanning : ConnectionUiState()
    data class AdaptersFound(val count: Int) : ConnectionUiState()
    object Connecting : ConnectionUiState()
    object Initializing : ConnectionUiState()
    data class Connected(val adapterInfo: AdapterInfo) : ConnectionUiState()
    data class Error(val message: String) : ConnectionUiState()
}

/**
 * One-time events for the connection screen.
 */
sealed class ConnectionEvent {
    data class ShowMessage(val message: String) : ConnectionEvent()
    data class ShowError(val message: String) : ConnectionEvent()
}
