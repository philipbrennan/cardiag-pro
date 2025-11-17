package com.cardiag.pro.ui.diagnostic

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cardiag.pro.R
import com.cardiag.pro.data.model.DiagnosticTroubleCode
import com.cardiag.pro.data.model.Severity
import com.cardiag.pro.databinding.ItemDtcCodeBinding

/**
 * Adapter for displaying DTC codes in a RecyclerView.
 */
class DtcListAdapter : ListAdapter<DiagnosticTroubleCode, DtcListAdapter.DtcViewHolder>(DtcDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DtcViewHolder {
        val binding = ItemDtcCodeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DtcViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DtcViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DtcViewHolder(
        private val binding: ItemDtcCodeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dtc: DiagnosticTroubleCode) {
            binding.tvDtcCode.text = dtc.code
            binding.tvDtcDescription.text = dtc.description
            binding.tvDtcSystem.text = "System: ${dtc.system.displayName}"

            // Set severity chip
            binding.chipSeverity.text = dtc.severity.displayName

            val chipColor = when (dtc.severity) {
                Severity.LOW -> R.color.severity_low
                Severity.MEDIUM -> R.color.severity_medium
                Severity.HIGH -> R.color.severity_high
                Severity.CRITICAL -> R.color.severity_critical
            }

            binding.chipSeverity.setChipBackgroundColorResource(chipColor)

            // Display possible causes if available
            if (!dtc.possibleCauses.isNullOrBlank()) {
                binding.tvPossibleCauses.text = "Possible Causes: ${dtc.possibleCauses}"
                binding.tvPossibleCauses.visibility = android.view.View.VISIBLE
            } else {
                binding.tvPossibleCauses.visibility = android.view.View.GONE
            }

            // Display source ECU if available
            if (dtc.sourceECU != null) {
                binding.chipEcu.text = dtc.sourceECU.displayName
                binding.chipEcu.visibility = android.view.View.VISIBLE
            } else {
                binding.chipEcu.visibility = android.view.View.GONE
            }
        }
    }

    private class DtcDiffCallback : DiffUtil.ItemCallback<DiagnosticTroubleCode>() {
        override fun areItemsTheSame(
            oldItem: DiagnosticTroubleCode,
            newItem: DiagnosticTroubleCode
        ): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(
            oldItem: DiagnosticTroubleCode,
            newItem: DiagnosticTroubleCode
        ): Boolean {
            return oldItem == newItem
        }
    }
}
