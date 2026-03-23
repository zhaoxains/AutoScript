package com.auto.app.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.auto.app.data.model.Script
import com.auto.app.data.repository.AppRepository
import com.auto.app.engine.ExecutionEngine
import com.auto.app.service.AutoAccessibilityService
import com.auto.app.data.remote.response.RemoteCommand
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

@Singleton
class ScriptScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository,
    private val executionEngine: ExecutionEngine,
    private val statusManager: ExecutionStatusManager
) {
    private val schedulerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var schedulerJob: Job? = null
    private var heartbeatJob: Job? = null
    
    private val lock = ReentrantLock()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring
    
    private val _currentScript = MutableStateFlow<Script?>(null)
    val currentScript: StateFlow<Script?> = _currentScript
    
    private val _executionQueue = MutableStateFlow<List<Script>>(emptyList())
    val executionQueue: StateFlow<List<Script>> = _executionQueue
    
    private val _completedScripts = MutableStateFlow<Set<Int>>(emptySet())
    
    private var pendingCommands = mutableListOf<RemoteCommand>()
    
    companion object {
        @Volatile
        private var instance: ScriptScheduler? = null
        
        fun getInstance(): ScriptScheduler? = instance
        
        fun isMonitoring(): Boolean = instance?._isMonitoring?.value ?: false
    }
    
    init {
        instance = this
    }
    
    fun startMonitoring() {
        lock.withLock {
            if (_isMonitoring.value) return
            
            _isMonitoring.value = true
            _completedScripts.value = emptySet()
            
            startHeartbeat()
            statusManager.setStatus("监控中", "等待命令")
        }
    }
    
    private fun startHeartbeat() {
        Log.d("ScriptScheduler", "startHeartbeat called")
        heartbeatJob = schedulerScope.launch {
            while (_isMonitoring.value) {
                try {
                    val deviceId = repository.getDeviceId() ?: ""
                    val status = if (_currentScript.value != null) 2 else 1
                    val currentTask = _currentScript.value?.scriptName
                    
                    Log.d("ScriptScheduler", "Sending heartbeat: deviceId=$deviceId, status=$status, currentTask=$currentTask")
                    
                    val response = repository.sendHeartbeat(deviceId, status, currentTask)
                    
                    Log.d("ScriptScheduler", "Heartbeat response: $response, commands count: ${response?.commands?.size ?: 0}")
                    
                    response?.commands?.forEach { command ->
                        Log.d("ScriptScheduler", "Received command: ${command.command}, params: ${command.params}")
                        handleRemoteCommand(command)
                    }
                    
                } catch (e: Exception) {
                    Log.e("ScriptScheduler", "Heartbeat error", e)
                }
                
                delay(3000)
            }
        }
    }
    
    private suspend fun handleRemoteCommand(command: RemoteCommand) {
        Log.d("ScriptScheduler", "handleRemoteCommand: ${command.command}")
        when (command.command) {
            "execute_script" -> {
                val scriptId = (command.params?.get("script_id") as? Number)?.toInt()
                Log.d("ScriptScheduler", "execute_script command, scriptId=$scriptId")
                if (scriptId != null) {
                    val scripts = fetchOnlineScripts()
                    Log.d("ScriptScheduler", "Fetched ${scripts.size} online scripts")
                    val script = scripts.find { it.scriptId == scriptId }
                    script?.let {
                        Log.d("ScriptScheduler", "Found script: ${it.scriptName}, calling forceExecuteScripts")
                        forceExecuteScripts(listOf(it))
                    } ?: Log.w("ScriptScheduler", "Script not found with id=$scriptId")
                }
            }
            "stop_execution" -> {
                Log.d("ScriptScheduler", "stop_execution command received")
                stopMonitoring()
            }
            "take_screenshot" -> {
                Log.d("ScriptScheduler", "take_screenshot command received")
                takeScreenshot()
            }
        }
    }
    
    private fun takeScreenshot() {
        Log.d("ScriptScheduler", "takeScreenshot called")
        
        if (!ScreenCaptureService.hasPermission()) {
            Log.e("ScriptScheduler", "No MediaProjection permission, need to request from Activity")
            return
        }
        
        ScreenCaptureService.setOnScreenshotCallback { success ->
            if (success) {
                Log.d("ScriptScheduler", "Screenshot captured and uploaded successfully")
            } else {
                Log.e("ScriptScheduler", "Screenshot capture failed")
            }
        }
        
        val intent = Intent(context, ScreenCaptureService::class.java)
        intent.action = "CAPTURE"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    fun stopMonitoring() {
        lock.withLock {
            _isMonitoring.value = false
            schedulerJob?.cancel()
            schedulerJob = null
            heartbeatJob?.cancel()
            heartbeatJob = null
            _currentScript.value = null
            _executionQueue.value = emptyList()
            _completedScripts.value = emptySet()
            pendingCommands.clear()
            executionEngine.stopExecution()
            statusManager.setStatus("空闲")
        }
    }
    
    fun reset() {
        lock.withLock {
            stopMonitoring()
            statusManager.reset()
        }
    }
    
    fun forceExecuteScripts(scripts: List<Script>) {
        if (scripts.isEmpty()) {
            Log.w("ScriptScheduler", "forceExecuteScripts: scripts list is empty!")
            return
        }
        
        Log.d("ScriptScheduler", "forceExecuteScripts called with ${scripts.size} scripts")
        
        val wasMonitoring = _isMonitoring.value
        
        stopSchedulerJob()
        
        schedulerScope.launch {
            Log.d("ScriptScheduler", "Coroutine started in forceExecuteScripts")
            
            delay(500)
            
            _isMonitoring.value = true
            _executionQueue.value = scripts
            
            Log.d("ScriptScheduler", "Starting execution loop for ${scripts.size} scripts")
            
            for ((index, script) in scripts.withIndex()) {
                Log.d("ScriptScheduler", "Loop iteration $index, isMonitoring=${_isMonitoring.value}")
                
                if (!_isMonitoring.value) {
                    Log.w("ScriptScheduler", "Breaking loop - monitoring stopped")
                    break
                }
                
                _currentScript.value = script
                Log.d("ScriptScheduler", "Executing script: ${script.scriptName}")
                
                try {
                    executeScriptWithRestart(script)
                    Log.d("ScriptScheduler", "Script completed: ${script.scriptName}")
                } catch (e: Exception) {
                    Log.e("ScriptScheduler", "Script failed: ${script.scriptName}", e)
                }
            }
            
            Log.d("ScriptScheduler", "Execution loop finished")
            
            _currentScript.value = null
            _executionQueue.value = emptyList()
            
            if (wasMonitoring) {
                _isMonitoring.value = true
                statusManager.setStatus("监控中", "等待命令")
            } else {
                _isMonitoring.value = false
                statusManager.setStatus("空闲")
            }
        }
    }
    
    private fun stopSchedulerJob() {
        schedulerJob?.cancel()
        schedulerJob = null
        executionEngine.stopExecution()
    }
    
    private suspend fun fetchOnlineScripts(): List<Script> {
        val result = repository.fetchScripts()
        return result.fold(
            onSuccess = { (scripts, _) -> scripts.filter { it.isOnline() } },
            onFailure = { repository.getScripts().filter { it.isOnline() } }
        )
    }
    
    private suspend fun executeScriptWithRestart(script: Script) {
        try {
            statusManager.setStatus("执行中", script.scriptName)
            
            killTargetApp(script.targetPackage)
            delay(1000)
            
            val result = executionEngine.executeScript(script)
            
            result.fold(
                onSuccess = {
                    repository.insertLog(
                        com.auto.app.data.model.ExecutionLog(
                            taskId = 0,
                            logType = com.auto.app.data.model.LogType.RESULT,
                            logContent = "脚本执行完成: ${script.scriptName}"
                        )
                    )
                },
                onFailure = { error ->
                    repository.insertLog(
                        com.auto.app.data.model.ExecutionLog(
                            taskId = 0,
                            logType = com.auto.app.data.model.LogType.ERROR,
                            logContent = "脚本执行失败: ${script.scriptName}",
                            detail = error.message
                        )
                    )
                }
            )
            
            killTargetApp(script.targetPackage)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun killTargetApp(packageName: String) {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.killBackgroundProcesses(packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun clearCompletedRecords() {
        _completedScripts.value = emptySet()
    }
    
    private val _isRunning: StateFlow<Boolean> get() = _isMonitoring
}
