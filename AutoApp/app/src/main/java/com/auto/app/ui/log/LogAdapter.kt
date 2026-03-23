package com.auto.app.ui.log

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.auto.app.data.model.ExecutionLog
import com.auto.app.data.model.LogType
import com.auto.app.databinding.ItemLogBinding
import com.auto.app.util.TimeUtils

class LogAdapter : ListAdapter<ExecutionLog, LogAdapter.ViewHolder>(LogDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLogBinding.inflate(
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
        private val binding: ItemLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(log: ExecutionLog) {
            binding.tvTime.text = TimeUtils.formatTime(log.logTime)
            binding.tvType.text = LogType.getLogTypeName(log.logType)
            binding.tvContent.text = log.logContent
            
            log.detail?.let {
                binding.tvDetail.text = it
                binding.tvDetail.visibility = android.view.View.VISIBLE
            } ?: run {
                binding.tvDetail.visibility = android.view.View.GONE
            }
            
            val textColor = when (log.logType) {
                LogType.ERROR -> Color.parseColor("#F44336")
                LogType.RESULT -> Color.parseColor("#4CAF50")
                else -> Color.parseColor("#212121")
            }
            binding.tvContent.setTextColor(textColor)
        }
    }
    
    class LogDiffCallback : DiffUtil.ItemCallback<ExecutionLog>() {
        override fun areItemsTheSame(oldItem: ExecutionLog, newItem: ExecutionLog): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ExecutionLog, newItem: ExecutionLog): Boolean {
            return oldItem == newItem
        }
    }
}
