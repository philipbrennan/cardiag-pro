package com.cardiag.pro.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cardiag.pro.data.local.entity.DiagnosticSessionEntity
import com.cardiag.pro.databinding.ItemDiagnosticSessionBinding
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying diagnostic session history.
 */
class DiagnosticSessionAdapter(
    private val onSessionClick: (DiagnosticSessionEntity) -> Unit
) : ListAdapter<DiagnosticSessionEntity, DiagnosticSessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemDiagnosticSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding, onSessionClick)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SessionViewHolder(
        private val binding: ItemDiagnosticSessionBinding,
        private val onSessionClick: (DiagnosticSessionEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())

        fun bind(session: DiagnosticSessionEntity) {
            binding.root.setOnClickListener { onSessionClick(session) }

            // Format date
            binding.tvSessionDate.text = dateFormat.format(Date(session.timestamp))

            // VIN
            binding.tvSessionVin.text = if (session.vin != null) {
                "VIN: ${session.vin}"
            } else {
                "VIN: Not recorded"
            }

            // Code count
            val codeCount = try {
                JSONArray(session.codes).length()
            } catch (e: Exception) {
                0
            }
            binding.chipCodeCount.text = "$codeCount code${if (codeCount != 1) "s" else ""}"

            // Manufacturer
            if (session.manufacturer != null) {
                binding.chipManufacturer.text = session.manufacturer
                binding.chipManufacturer.isVisible = true
            } else {
                binding.chipManufacturer.isVisible = false
            }

            // System scanned
            if (session.systemScanned != null) {
                binding.chipSystem.text = session.systemScanned
                binding.chipSystem.isVisible = true
            } else {
                binding.chipSystem.isVisible = false
            }

            // Freeze frame indicator
            if (!session.freezeFrames.isNullOrBlank()) {
                binding.tvFreezeFrameIndicator.text = "Freeze frame data available"
                binding.tvFreezeFrameIndicator.isVisible = true
            } else {
                binding.tvFreezeFrameIndicator.isVisible = false
            }

            // Notes
            if (!session.notes.isNullOrBlank()) {
                binding.tvNotes.text = session.notes
                binding.tvNotes.isVisible = true
            } else {
                binding.tvNotes.isVisible = false
            }
        }
    }

    private class SessionDiffCallback : DiffUtil.ItemCallback<DiagnosticSessionEntity>() {
        override fun areItemsTheSame(
            oldItem: DiagnosticSessionEntity,
            newItem: DiagnosticSessionEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: DiagnosticSessionEntity,
            newItem: DiagnosticSessionEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
