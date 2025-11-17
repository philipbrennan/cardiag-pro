package com.cardiag.pro.ui.history

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
import com.cardiag.pro.databinding.FragmentHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Fragment displaying diagnostic session history.
 */
@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private val sessionAdapter = DiagnosticSessionAdapter { session ->
        // TODO: Navigate to session detail
        Timber.d("Clicked session: ${session.id}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeState()
    }

    private fun setupRecyclerView() {
        binding.rvSessions.apply {
            adapter = sessionAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupClickListeners() {
        binding.btnClearHistory.setOnClickListener {
            showClearHistoryDialog()
        }

        binding.btnExportHistory.setOnClickListener {
            viewModel.exportHistory()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.sessions.collect { sessions ->
                        sessionAdapter.submitList(sessions)
                        
                        binding.layoutEmptyState.isVisible = sessions.isEmpty()
                        binding.rvSessions.isVisible = sessions.isNotEmpty()
                        
                        // Update statistics
                        binding.tvTotalSessions.text = sessions.size.toString()
                        
                        val totalCodes = sessions.sumOf { session ->
                            try {
                                org.json.JSONArray(session.codes).length()
                            } catch (e: Exception) {
                                0
                            }
                        }
                        binding.tvTotalCodes.text = totalCodes.toString()
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is HistoryUiState.Idle -> {
                                // Do nothing
                            }
                            is HistoryUiState.ExportSuccess -> {
                                showMessage("History exported successfully")
                            }
                            is HistoryUiState.Error -> {
                                showMessage("Error: ${state.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showClearHistoryDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear All History?")
            .setMessage("This will permanently delete all diagnostic sessions. This cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                viewModel.clearAllHistory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMessage(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
