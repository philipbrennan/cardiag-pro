package com.cardiag.pro.ui.diagnostic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.cardiag.pro.R
import com.cardiag.pro.data.model.Manufacturer
import com.cardiag.pro.databinding.FragmentDiagnosticBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment for diagnostic operations (VIN reading and DTC code reading).
 */
@AndroidEntryPoint
class DiagnosticFragment : Fragment() {

    private var _binding: FragmentDiagnosticBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiagnosticViewModel by viewModels()
    private val dtcAdapter = DtcListAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiagnosticBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeState()
    }

    private fun setupRecyclerView() {
        binding.rvDtcCodes.apply {
            adapter = dtcAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupClickListeners() {
        binding.btnReadVin.setOnClickListener {
            viewModel.readVin()
        }

        binding.btnManualEntry.setOnClickListener {
            showManufacturerSelectionDialog()
        }

        binding.btnReadCodes.setOnClickListener {
            viewModel.readDtcCodes()
        }

        binding.btnClearCodes.setOnClickListener {
            showClearCodesConfirmationDialog()
        }

        binding.btnSaveSession.setOnClickListener {
            viewModel.saveDiagnosticSession()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }

                launch {
                    viewModel.vehicleInfo.collect { vehicleInfo ->
                        if (vehicleInfo != null) {
                            binding.tvVin.text = "VIN: ${vehicleInfo.vin}"
                            binding.tvManufacturer.text = "Manufacturer: ${vehicleInfo.manufacturer.displayName}"
                            
                            // Show manufacturer chip
                            binding.chipManufacturer.text = vehicleInfo.manufacturer.displayName
                            binding.chipManufacturer.visibility = android.view.View.VISIBLE
                            
                            // Set chip color based on manufacturer
                            val chipBackgroundColor = when (vehicleInfo.manufacturer) {
                                com.cardiag.pro.data.model.Manufacturer.BMW -> R.color.manufacturer_bmw
                                com.cardiag.pro.data.model.Manufacturer.VOLKSWAGEN -> R.color.manufacturer_vw
                                com.cardiag.pro.data.model.Manufacturer.NISSAN -> R.color.manufacturer_nissan
                                else -> R.color.manufacturer_generic
                            }
                            binding.chipManufacturer.setChipBackgroundColorResource(chipBackgroundColor)
                        } else {
                            binding.tvVin.text = "VIN: Not detected"
                            binding.tvManufacturer.text = "Manufacturer: Unknown"
                            binding.chipManufacturer.visibility = android.view.View.GONE
                        }
                    }
                }

                launch {
                    viewModel.dtcCodes.collect { codes ->
                        dtcAdapter.submitList(codes)

                        if (codes.isEmpty()) {
                            binding.tvCodeCount.text = "No codes detected"
                            binding.rvDtcCodes.isVisible = false
                        } else {
                            binding.tvCodeCount.text = "${codes.size} code(s) detected"
                            binding.rvDtcCodes.isVisible = true
                        }
                    }
                }
            }
        }
    }

    private fun handleUiState(state: DiagnosticUiState) {
        when (state) {
            is DiagnosticUiState.Idle -> {
                binding.progressIndicator.isVisible = false
            }

            is DiagnosticUiState.ReadingVin -> {
                binding.progressIndicator.isVisible = true
                showSnackbar("Reading VIN from vehicle...")
            }

            is DiagnosticUiState.VinReadSuccess -> {
                binding.progressIndicator.isVisible = false
                showSnackbar("VIN read successfully: ${state.vehicleInfo.manufacturer.displayName}")
                viewModel.resetState()
            }

            is DiagnosticUiState.ReadingCodes -> {
                binding.progressIndicator.isVisible = true
                showSnackbar("Reading diagnostic codes...")
            }

            is DiagnosticUiState.CodesReadSuccess -> {
                binding.progressIndicator.isVisible = false
                showSnackbar("${state.count} code(s) found")
                viewModel.resetState()
            }

            is DiagnosticUiState.NoCodesFound -> {
                binding.progressIndicator.isVisible = false
                showSnackbar("No trouble codes found - vehicle is healthy!")
                viewModel.resetState()
            }

            is DiagnosticUiState.ClearingCodes -> {
                binding.progressIndicator.isVisible = true
                showSnackbar("Clearing codes...")
            }

            is DiagnosticUiState.CodesCleared -> {
                binding.progressIndicator.isVisible = false
                showSnackbar("Codes cleared successfully")
                viewModel.resetState()
            }

            is DiagnosticUiState.SessionSaved -> {
                binding.progressIndicator.isVisible = false
                showSnackbar("Diagnostic session saved")
                viewModel.resetState()
            }

            is DiagnosticUiState.Error -> {
                binding.progressIndicator.isVisible = false
                showSnackbar("Error: ${state.message}", isError = true)
                viewModel.resetState()
            }
        }
    }

    private fun showManufacturerSelectionDialog() {
        val manufacturers = arrayOf("BMW", "Volkswagen", "Nissan")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Manufacturer")
            .setItems(manufacturers) { _, which ->
                val manufacturer = when (which) {
                    0 -> Manufacturer.BMW
                    1 -> Manufacturer.VOLKSWAGEN
                    2 -> Manufacturer.NISSAN
                    else -> Manufacturer.UNKNOWN
                }
                viewModel.setManualVehicleInfo(manufacturer)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearCodesConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear Diagnostic Codes?")
            .setMessage("This will clear all trouble codes from the vehicle's ECU and turn off the check engine light.")
            .setPositiveButton("Clear") { _, _ ->
                viewModel.clearDtcCodes()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSnackbar(message: String, isError: Boolean = false) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .apply {
                if (isError) {
                    setBackgroundTint(resources.getColor(R.color.severity_high, null))
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
