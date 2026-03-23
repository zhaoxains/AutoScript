package com.auto.app.ui.log

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.auto.app.databinding.ActivityLogDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LogDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLogDetailBinding
    private val viewModel: LogDetailViewModel by viewModels()
    private lateinit var logAdapter: LogAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val recordId = intent.getLongExtra("record_id", 0)
        if (recordId > 0) {
            viewModel.loadRecord(recordId)
        }
        
        setupViews()
        observeViewModel()
    }
    
    private fun setupViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        logAdapter = LogAdapter()
        binding.rvLogs.apply {
            layoutManager = LinearLayoutManager(this@LogDetailActivity)
            adapter = logAdapter
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.record.collect { record ->
                record?.let {
                    binding.tvTaskName.text = it.taskName
                    binding.tvStatus.text = com.auto.app.data.model.TaskStatus.getStatusName(it.status)
                    binding.tvDuration.text = "耗时: ${com.auto.app.util.TimeUtils.formatDuration(it.duration)}"
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.logs.collect { logs ->
                logAdapter.submitList(logs)
            }
        }
    }
}
