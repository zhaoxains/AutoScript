package com.auto.app.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auto.app.data.model.Script
import com.auto.app.data.model.TaskRecord
import com.auto.app.data.repository.AppRepository
import com.auto.app.engine.ExecutionEngine
import com.auto.app.service.ExecutionStatusManager
import com.auto.app.service.ScriptScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRepository,
    private val executionEngine: ExecutionEngine,
    private val statusManager: ExecutionStatusManager,
    private val scriptScheduler: ScriptScheduler
) : ViewModel() {
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    private val _scripts = MutableStateFlow<List<Script>>(emptyList())
    val scripts: StateFlow<List<Script>> = _scripts.asStateFlow()
    
    private val _records = MutableStateFlow<List<TaskRecord>>(emptyList())
    val records: StateFlow<List<TaskRecord>> = _records.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private val _currentStatus = MutableStateFlow("空闲")
    val currentStatus: StateFlow<String> = _currentStatus.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _currentScript = MutableStateFlow<Script?>(null)
    val currentScript: StateFlow<Script?> = _currentScript.asStateFlow()
    
    private val _executionQueue = MutableStateFlow<List<Script>>(emptyList())
    val executionQueue: StateFlow<List<Script>> = _executionQueue.asStateFlow()
    
    private val _selectedScript = MutableStateFlow<Script?>(null)
    
    init {
        refreshData()
        observeStatus()
        observeRecords()
        observeScheduler()
    }
    
    private fun observeStatus() {
        statusManager.setStatusListener { status, taskName ->
            _currentStatus.value = if (taskName != null) "$status: $taskName" else status
        }
    }
    
    private fun observeRecords() {
        viewModelScope.launch {
            repository.getAllRecords().collect { records ->
                _records.value = records
            }
        }
    }
    
    private fun observeScheduler() {
        viewModelScope.launch {
            scriptScheduler.isMonitoring.collect { isMonitoring ->
                _isMonitoring.value = isMonitoring
                _isRunning.value = isMonitoring
            }
        }
        
        viewModelScope.launch {
            scriptScheduler.currentScript.collect { script ->
                _currentScript.value = script
            }
        }
        
        viewModelScope.launch {
            scriptScheduler.executionQueue.collect { queue ->
                _executionQueue.value = queue
            }
        }
    }
    
    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = repository.fetchScripts()
            result.fold(
                onSuccess = { (scripts, _) ->
                    _scripts.value = scripts
                    _error.value = null
                },
                onFailure = { error ->
                    _error.value = error.message
                    _scripts.value = repository.getScripts()
                }
            )
            
            _isLoading.value = false
        }
    }
    
    fun selectScript(script: Script) {
        _selectedScript.value = script
    }
    
    fun getSelectedScript(): Script? = _selectedScript.value
    
    fun startMonitoring() {
        if (_isMonitoring.value) {
            scriptScheduler.stopMonitoring()
        } else {
            scriptScheduler.startMonitoring()
        }
    }
    
    fun stopMonitoring() {
        scriptScheduler.stopMonitoring()
    }
    
    fun executeScript(script: Script) {
        Log.d(TAG, "executeScript called for: ${script.scriptName}")
        scriptScheduler.forceExecuteScripts(listOf(script))
    }
    
    fun executeAllScripts() {
        val onlineScripts = _scripts.value.filter { it.isOnline() }
        Log.d(TAG, "executeAllScripts called, online scripts count: ${onlineScripts.size}")
        Log.d(TAG, "Total scripts: ${_scripts.value.size}")
        _scripts.value.forEach { script ->
            Log.d(TAG, "Script: ${script.scriptName}, status: ${script.status}, isOnline: ${script.isOnline()}")
        }
        
        if (onlineScripts.isNotEmpty()) {
            Log.d(TAG, "Calling scriptScheduler.forceExecuteScripts with ${onlineScripts.size} scripts")
            scriptScheduler.forceExecuteScripts(onlineScripts)
        } else {
            Log.w(TAG, "No online scripts to execute!")
        }
    }
    
    fun stopExecution() {
        scriptScheduler.stopMonitoring()
        _isRunning.value = false
        _currentStatus.value = "已停止"
    }
    
    fun isLoggedIn(): Boolean = repository.isLoggedIn()
}
