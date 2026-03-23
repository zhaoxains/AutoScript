package com.auto.app.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.auto.app.data.model.Script
import com.auto.app.databinding.ItemScriptBinding

class ScriptAdapter(
    private val onItemClick: (Script) -> Unit,
    private val onExecuteClick: ((Script) -> Unit)? = null
) : ListAdapter<Script, ScriptAdapter.ViewHolder>(ScriptDiffCallback()) {
    
    private var selectedId: Int? = null
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScriptBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val script = getItem(position)
        holder.bind(script, script.scriptId == selectedId)
    }
    
    fun setSelected(scriptId: Int) {
        selectedId = scriptId
        notifyDataSetChanged()
    }
    
    inner class ViewHolder(
        private val binding: ItemScriptBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val script = getItem(bindingAdapterPosition)
                selectedId = script.scriptId
                onItemClick(script)
                notifyDataSetChanged()
            }
            
            binding.root.setOnLongClickListener {
                val script = getItem(bindingAdapterPosition)
                onExecuteClick?.invoke(script)
                true
            }
        }
        
        fun bind(script: Script, isSelected: Boolean) {
            binding.tvName.text = script.scriptName
            binding.tvPackage.text = script.targetPackage
            binding.root.isSelected = isSelected
            
            if (isSelected) {
                binding.root.strokeWidth = 2
            } else {
                binding.root.strokeWidth = 0
            }
        }
    }
    
    class ScriptDiffCallback : DiffUtil.ItemCallback<Script>() {
        override fun areItemsTheSame(oldItem: Script, newItem: Script): Boolean {
            return oldItem.scriptId == newItem.scriptId
        }
        
        override fun areContentsTheSame(oldItem: Script, newItem: Script): Boolean {
            return oldItem == newItem
        }
    }
}
