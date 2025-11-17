package com.cardiag.pro.ui.logs

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.cardiag.pro.R
import com.cardiag.pro.data.model.LogLevel
import com.cardiag.pro.databinding.FragmentLogsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

/**
 * Fragment for viewing and managing application logs.
 */
@AndroidEntryPoint
class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LogsViewModel by viewModels()
    private val logAdapter = LogEntryAdapter()

    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilterChips()
        setupButtons()
        observeState()
    }

    private fun setupRecyclerView() {
        layoutManager = LinearLayoutManager(context)
        binding.rvLogs.apply {
            adapter = logAdapter
            this.layoutManager = this@LogsFragment.layoutManager
            setHasFixedSize(true)
        }
    }

    private fun setupFilterChips() {
        binding.chipGroupLogLevel.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chipAll -> viewModel.setFilter(null)
                R.id.chipDebug -> viewModel.setFilter(LogLevel.DEBUG)
                R.id.chipInfo -> viewModel.setFilter(LogLevel.INFO)
                R.id.chipWarn -> viewModel.setFilter(LogLevel.WARN)
                R.id.chipError -> viewModel.setFilter(LogLevel.ERROR)
            }
        }
    }

    private fun setupButtons() {
        binding.btnClearLogs.setOnClickListener {
            showClearLogsConfirmation()
        }

        binding.btnExportLogs.setOnClickListener {
            viewModel.exportLogs()
        }

        binding.fabScrollToBottom.setOnClickListener {
            scrollToBottom()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe filtered logs
                launch {
                    viewModel.filteredLogs.collect { logs ->
                        logAdapter.submitList(logs) {
                            // Auto-scroll to bottom on new logs if already at bottom
                            if (shouldAutoScroll()) {
                                scrollToBottom()
                            }
                        }

                        // Update UI
                        binding.emptyState.isVisible = logs.isEmpty()
                        binding.rvLogs.isVisible = logs.isNotEmpty()

                        // Update count
                        val stats = viewModel.getLogStats()
                        binding.tvLogCount.text = "${logs.size} of ${stats.total} log entries"
                    }
                }

                // Observe UI state
                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }
            }
        }
    }

    private fun handleUiState(state: LogsUiState) {
        when (state) {
            is LogsUiState.Idle -> {
                // Nothing to do
            }

            is LogsUiState.Exporting -> {
                showSnackbar("Exporting logs...")
            }

            is LogsUiState.ExportSuccess -> {
                shareLogFile(state.filePath)
                viewModel.resetState()
            }

            is LogsUiState.ExportError -> {
                showSnackbar("Error: ${state.message}", isError = true)
                viewModel.resetState()
            }

            is LogsUiState.LogsCleared -> {
                showSnackbar("Logs cleared")
                viewModel.resetState()
            }
        }
    }

    private fun showClearLogsConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear All Logs?")
            .setMessage("This will delete all log files and clear the log viewer. This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                viewModel.clearLogs()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareLogFile(filePath: String) {
        try {
            val logFile = File(filePath)
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                logFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "CarDiag Pro Logs")
                putExtra(Intent.EXTRA_TEXT, "CarDiag Pro diagnostic logs attached")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Logs"))
        } catch (e: Exception) {
            showSnackbar("Failed to share logs: ${e.message}", isError = true)
        }
    }

    private fun scrollToBottom() {
        if (logAdapter.itemCount > 0) {
            binding.rvLogs.smoothScrollToPosition(logAdapter.itemCount - 1)
        }
    }

    private fun shouldAutoScroll(): Boolean {
        val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
        val lastItemPosition = logAdapter.itemCount - 1
        return lastVisiblePosition >= lastItemPosition - 1 // Auto-scroll if near bottom
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
