package com.cardiag.pro.ui.connection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.cardiag.pro.R
import com.cardiag.pro.data.model.ConnectionState
import com.cardiag.pro.databinding.FragmentConnectionBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ConnectionFragment : Fragment() {

    private var _binding: FragmentConnectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConnectionViewModel by viewModels()
    private lateinit var adapterListAdapter: AdapterListAdapter

    // Permission launcher for Bluetooth
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Timber.d("Bluetooth permissions granted")
            viewModel.scanBluetooth()
        } else {
            Timber.w("Bluetooth permissions denied")
            showError("Bluetooth permissions are required to scan for adapters")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapterListAdapter = AdapterListAdapter { adapterInfo ->
            Timber.d("Adapter clicked: ${adapterInfo.name}")
            viewModel.connect(adapterInfo)
        }

        binding.adaptersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adapterListAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnScanBluetooth.setOnClickListener {
            if (hasBluetoothPermissions()) {
                viewModel.scanBluetooth()
            } else {
                requestBluetoothPermissions()
            }
        }

        binding.btnScanUsb.setOnClickListener {
            viewModel.scanUsb()
        }

        binding.btnDisconnect.setOnClickListener {
            viewModel.disconnect()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe UI state
                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }

                // Observe adapters list
                launch {
                    viewModel.adapters.collect { adapters ->
                        adapterListAdapter.submitList(adapters)
                    }
                }

                // Observe connection state
                launch {
                    viewModel.connectionState.collect { state ->
                        handleConnectionState(state)
                    }
                }

                // Observe events
                launch {
                    viewModel.events.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun handleUiState(state: ConnectionUiState) {
        when (state) {
            is ConnectionUiState.Idle -> {
                binding.progressBar.visibility = View.GONE
                binding.errorText.visibility = View.GONE
                binding.btnScanBluetooth.isEnabled = true
                binding.btnScanUsb.isEnabled = true
            }
            is ConnectionUiState.Scanning -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.errorText.visibility = View.GONE
                binding.btnScanBluetooth.isEnabled = false
                binding.btnScanUsb.isEnabled = false
            }
            is ConnectionUiState.AdaptersFound -> {
                binding.progressBar.visibility = View.GONE
                binding.errorText.visibility = View.GONE
                binding.btnScanBluetooth.isEnabled = true
                binding.btnScanUsb.isEnabled = true
            }
            is ConnectionUiState.Connecting -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.errorText.visibility = View.GONE
                binding.connectionStatus.text = getString(R.string.connecting)
            }
            is ConnectionUiState.Initializing -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.connectionStatus.text = "Initializing adapter..."
            }
            is ConnectionUiState.Connected -> {
                binding.progressBar.visibility = View.GONE
                binding.errorText.visibility = View.GONE
                binding.connectionStatus.text = getString(R.string.connected)
                binding.btnDisconnect.visibility = View.VISIBLE
                binding.btnScanBluetooth.isEnabled = false
                binding.btnScanUsb.isEnabled = false
            }
            is ConnectionUiState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.errorText.visibility = View.VISIBLE
                binding.errorText.text = "Error: ${state.message}"
                binding.btnScanBluetooth.isEnabled = true
                binding.btnScanUsb.isEnabled = true
            }
        }
    }

    private fun handleConnectionState(state: ConnectionState) {
        when (state) {
            is ConnectionState.Disconnected -> {
                binding.connectionStatus.text = getString(R.string.disconnected)
                binding.btnDisconnect.visibility = View.GONE
                binding.btnScanBluetooth.isEnabled = true
                binding.btnScanUsb.isEnabled = true
            }
            is ConnectionState.Scanning -> {
                // Handled by UI state
            }
            is ConnectionState.Connecting -> {
                binding.connectionStatus.text = getString(R.string.connecting)
            }
            is ConnectionState.Connected -> {
                binding.connectionStatus.text = "${getString(R.string.connected)} - ${state.adapterInfo.name}"
            }
            is ConnectionState.Error -> {
                // Handled by UI state
            }
        }
    }

    private fun handleEvent(event: ConnectionEvent) {
        when (event) {
            is ConnectionEvent.ShowMessage -> {
                showMessage(event.message)
            }
            is ConnectionEvent.ShowError -> {
                showError(event.message)
            }
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(
                ContextCompat.getColor(requireContext(), com.google.android.material.R.color.design_default_color_error)
            )
            .show()
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11 and below
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        bluetoothPermissionLauncher.launch(permissions)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
