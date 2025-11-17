package com.cardiag.pro.ui.monitoring

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cardiag.pro.databinding.FragmentMonitoringBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment for real-time OBD2 sensor monitoring.
 */
@AndroidEntryPoint
class MonitoringFragment : Fragment() {

    private var _binding: FragmentMonitoringBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MonitoringViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonitoringBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeState()
    }

    private fun setupClickListeners() {
        binding.btnToggleMonitoring.setOnClickListener {
            viewModel.toggleMonitoring()
        }

        binding.btnToggleUnits.setOnClickListener {
            viewModel.toggleUnits()
        }

        binding.chipRefreshRate1Hz.setOnClickListener {
            viewModel.setRefreshRate(1.0)
        }

        binding.chipRefreshRate2Hz.setOnClickListener {
            viewModel.setRefreshRate(2.0)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.liveData.collect { data ->
                        updateSensorDisplay(data)
                    }
                }

                launch {
                    viewModel.isMonitoring.collect { isMonitoring ->
                        updateMonitoringState(isMonitoring)
                    }
                }

                launch {
                    viewModel.refreshRate.collect { rate ->
                        updateRefreshRateChips(rate)
                    }
                }

                launch {
                    viewModel.useMetric.collect { metric ->
                        binding.btnToggleUnits.text = if (metric) "Metric" else "Imperial"
                    }
                }
            }
        }
    }

    private fun updateSensorDisplay(data: com.cardiag.pro.data.model.LiveSensorData) {
        binding.apply {
            tvRpm.text = data.rpm?.toString() ?: "--"
            tvSpeed.text = viewModel.formatSpeed(data.speed)
            tvCoolantTemp.text = viewModel.formatTemperature(data.coolantTemp)
            tvThrottle.text = data.throttlePosition?.let { "$it%" } ?: "--"
            tvEngineLoad.text = data.engineLoad?.let { "$it%" } ?: "--"
            tvShortTermFuel.text = data.shortTermFuelTrim?.let { "${"%.1f".format(it)}%" } ?: "--"
            tvLongTermFuel.text = data.longTermFuelTrim?.let { "${"%.1f".format(it)}%" } ?: "--"
            tvMaf.text = data.mafAirFlow?.let { "${"%.2f".format(it)} g/s" } ?: "--"
            tvIntakePressure.text = data.intakeManifoldPressure?.let { "$it kPa" } ?: "--"
            tvTimingAdvance.text = data.timingAdvance?.let { "${"%.1f".format(it)}°" } ?: "--"
        }
    }

    private fun updateMonitoringState(isMonitoring: Boolean) {
        if (isMonitoring) {
            binding.btnToggleMonitoring.text = "Stop Monitoring"
            binding.statusIndicator.setBackgroundResource(android.R.color.holo_green_light)
        } else {
            binding.btnToggleMonitoring.text = "Start Monitoring"
            binding.statusIndicator.setBackgroundResource(android.R.color.darker_gray)
        }
    }

    private fun updateRefreshRateChips(rate: Double) {
        binding.chipRefreshRate1Hz.isChecked = (rate == 1.0)
        binding.chipRefreshRate2Hz.isChecked = (rate == 2.0)
    }

    override fun onPause() {
        super.onPause()
        // Stop monitoring when fragment is paused to save battery
        viewModel.stopMonitoring()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
