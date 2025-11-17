package com.cardiag.pro.ui.logs

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cardiag.pro.data.model.LogEntry
import com.cardiag.pro.data.model.LogLevel
import com.cardiag.pro.databinding.ItemLogEntryBinding

/**
 * Adapter for displaying log entries in a RecyclerView.
 */
class LogEntryAdapter : ListAdapter<LogEntry, LogEntryAdapter.LogViewHolder>(LogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(
        private val binding: ItemLogEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var isExpanded = false

        fun bind(entry: LogEntry) {
            binding.apply {
                // Timestamp
                tvTimestamp.text = entry.getFormattedTime()

                // Level with color
                tvLevel.text = entry.level.displayName
                val levelColor = Color.parseColor(entry.level.colorCode)
                tvLevel.setTextColor(levelColor)
                levelIndicator.setBackgroundColor(levelColor)

                // Tag
                tvTag.text = entry.tag

                // Message
                tvMessage.text = entry.message

                // Metadata
                if (entry.metadata.isNotEmpty()) {
                    val metadataText = entry.metadata.entries.joinToString(", ") {
                        "${it.key}=${it.value}"
                    }
                    tvMetadata.text = metadataText
                    metadataContainer.isVisible = isExpanded
                } else {
                    metadataContainer.isVisible = false
                }

                // Exception
                if (entry.throwable != null) {
                    tvException.text = entry.throwable.stackTraceToString()
                    exceptionContainer.isVisible = isExpanded
                } else {
                    exceptionContainer.isVisible = false
                }

                // Click to expand/collapse
                root.setOnClickListener {
                    isExpanded = !isExpanded
                    metadataContainer.isVisible = isExpanded && entry.metadata.isNotEmpty()
                    exceptionContainer.isVisible = isExpanded && entry.throwable != null
                }

                // Set card background based on level
                when (entry.level) {
                    LogLevel.ERROR -> root.strokeColor = Color.parseColor("#F44336")
                    LogLevel.WARN -> root.strokeColor = Color.parseColor("#FF9800")
                    else -> root.strokeColor = Color.parseColor("#E0E0E0")
                }
            }
        }
    }

    private class LogDiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem == newItem
        }
    }
}
