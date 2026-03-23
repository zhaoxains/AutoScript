package com.auto.app.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.auto.app.data.model.TaskRecord
import com.auto.app.data.model.TaskStatus
import com.auto.app.databinding.ItemRecordBinding
import com.auto.app.util.TimeUtils

class RecordAdapter(
    private val onItemClick: (TaskRecord) -> Unit
) : ListAdapter<TaskRecord, RecordAdapter.ViewHolder>(RecordDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                onItemClick(getItem(bindingAdapterPosition))
            }
        }
        
        fun bind(record: TaskRecord) {
            binding.tvTaskName.text = record.taskName
            binding.tvTime.text = TimeUtils.formatTime(record.startTime)
            binding.tvDuration.text = TimeUtils.formatDuration(record.duration)
            
            val statusText = TaskStatus.getStatusName(record.status)
            val statusColor = when (record.status) {
                TaskStatus.SUCCESS -> com.auto.app.R.color.success
                TaskStatus.FAILED -> com.auto.app.R.color.error
                TaskStatus.INTERRUPTED -> com.auto.app.R.color.warning
                else -> com.auto.app.R.color.text_secondary
            }
            
            binding.tvStatus.text = statusText
            binding.tvStatus.setTextColor(binding.root.context.getColor(statusColor))
            
            if (record.status == TaskStatus.FAILED && record.errorMsg != null) {
                binding.tvError.text = record.errorMsg
                binding.tvError.visibility = android.view.View.VISIBLE
            } else {
                binding.tvError.visibility = android.view.View.GONE
            }
        }
    }
    
    class RecordDiffCallback : DiffUtil.ItemCallback<TaskRecord>() {
        override fun areItemsTheSame(oldItem: TaskRecord, newItem: TaskRecord): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: TaskRecord, newItem: TaskRecord): Boolean {
            return oldItem == newItem
        }
    }
}
