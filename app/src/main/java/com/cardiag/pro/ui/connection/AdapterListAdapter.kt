package com.cardiag.pro.ui.connection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cardiag.pro.data.model.AdapterInfo
import com.cardiag.pro.databinding.ItemAdapterBinding

/**
 * RecyclerView adapter for displaying OBD2 adapters.
 */
class AdapterListAdapter(
    private val onAdapterClick: (AdapterInfo) -> Unit
) : ListAdapter<AdapterInfo, AdapterListAdapter.AdapterViewHolder>(AdapterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterViewHolder {
        val binding = ItemAdapterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdapterViewHolder(binding, onAdapterClick)
    }

    override fun onBindViewHolder(holder: AdapterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AdapterViewHolder(
        private val binding: ItemAdapterBinding,
        private val onAdapterClick: (AdapterInfo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(adapter: AdapterInfo) {
            binding.adapterName.text = adapter.name
            binding.adapterType.text = "${adapter.type.name} - ${adapter.address ?: "N/A"}"
            binding.root.setOnClickListener {
                onAdapterClick(adapter)
            }
        }
    }

    private class AdapterDiffCallback : DiffUtil.ItemCallback<AdapterInfo>() {
        override fun areItemsTheSame(oldItem: AdapterInfo, newItem: AdapterInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AdapterInfo, newItem: AdapterInfo): Boolean {
            return oldItem == newItem
        }
    }
}
