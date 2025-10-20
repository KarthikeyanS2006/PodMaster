// File: RecordingsAdapter.kt
package com.podmaster.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.podmaster.databinding.ItemRecordingBinding
import com.podmaster.models.Recording

class RecordingsAdapter(
    private val onPlayClick: (Recording) -> Unit,
    private val onShareClick: (Recording) -> Unit,
    private val onDeleteClick: (Recording) -> Unit,
    private val onMoreClick: (Recording) -> Unit
) : ListAdapter<Recording, RecordingsAdapter.RecordingViewHolder>(RecordingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val binding = ItemRecordingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecordingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecordingViewHolder(
        private val binding: ItemRecordingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recording: Recording) {
            binding.tvName.text = recording.name
            binding.tvInfo.text = "${recording.getSizeInMB()} • ${recording.getDurationFormatted()} • ${recording.getFormattedDate()}"

            binding.btnPlay.setOnClickListener { onPlayClick(recording) }
            binding.btnShare.setOnClickListener { onShareClick(recording) }
            binding.btnDelete.setOnClickListener { onDeleteClick(recording) }
            binding.btnMore.setOnClickListener { onMoreClick(recording) }
        }
    }

    class RecordingDiffCallback : DiffUtil.ItemCallback<Recording>() {
        override fun areItemsTheSame(oldItem: Recording, newItem: Recording): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: Recording, newItem: Recording): Boolean {
            return oldItem == newItem
        }
    }
}
