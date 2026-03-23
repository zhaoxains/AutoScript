package com.auto.app.engine

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.auto.app.data.model.*
import com.auto.app.data.repository.AppRepository
import com.auto.app.service.AutoAccessibilityService
import com.auto.app.service.ExecutionStatusManager
import com.auto.app.util.TimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecutionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository,
    private val controlEngine: ControlEngine,
    private val adEngine: AdEngine,
    private val statusManager: ExecutionStatusManager
) {
    private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var currentJob: Job? = null
    
    private val _isRunning = MutableStateFlow(false)
    val isRunningState: StateFlow<Boolean> = _isRunning
    
    private val _currentTask = MutableStateFlow<TaskRecord?>(null)
    val currentTask: StateFlow<TaskRecord?> = _currentTask
    
    companion object {
        private const val TAG = "ExecutionEngine"
        
        @Volatile
        private var instance: ExecutionEngine? = null
        
        fun isRunning(): Boolean = instance?._isRunning?.value ?: false
    }
    
    init {
        instance = this
    }
    
    suspend fun executeScript(script: Script): Result<TaskRecord> {
        Log.d(TAG, "executeScript called for: ${script.scriptName}")
        Log.d(TAG, "_isRunning.value = ${_isRunning.value}")
        
        if (_isRunning.value) {
            Log.w(TAG, "已有任务在执行中，返回失败")
            return Result.failure(Exception("已有任务在执行中"))
        }
        
        val serviceEnabled = AutoAccessibilityService.isServiceEnabled()
        Log.d(TAG, "AutoAccessibilityService.isServiceEnabled() = $serviceEnabled")
        
        if (!serviceEnabled) {
            Log.w(TAG, "无障碍服务未开启，返回失败")
            return Result.failure(Exception("无障碍服务未开启"))
        }
        
        val user = repository.getUser()
        Log.d(TAG, "repository.getUser() = $user")
        if (user == null) {
            Log.w(TAG, "用户未登录，返回失败")
            return Result.failure(Exception("用户未登录"))
        }
        
        val deviceId = repository.getDeviceId()
        Log.d(TAG, "repository.getDeviceId() = $deviceId")
        if (deviceId == null) {
            Log.w(TAG, "设备ID不存在，返回失败")
            return Result.failure(Exception("设备ID不存在"))
        }
        
        Log.d(TAG, "开始执行脚本: ${script.scriptName}")
        
        val record = TaskRecord(
            userId = user.userId,
            deviceId = deviceId,
            scriptId = script.scriptId,
            taskName = script.scriptName,
            startTime = System.currentTimeMillis(),
            status = TaskStatus.RUNNING
        )
        
        val recordId = repository.insertRecord(record)
        val savedRecord = record.copy(id = recordId)
        _currentTask.value = savedRecord
        _isRunning.value = true
        
        var result: Result<TaskRecord> = Result.success(savedRecord)
        
        currentJob = engineScope.launch {
            try {
                statusManager.setStatus("执行中", script.scriptName)
                
                repository.insertLog(
                    ExecutionLog(
                        taskId = recordId,
                        logType = LogType.TASK_INFO,
                        logContent = "开始执行脚本: ${script.scriptName}"
                    )
                )
                
                if (!isAppInstalled(script.targetPackage)) {
                    throw ExecutionException(ErrorCode.APP_NOT_INSTALLED, "目标APP未安装: ${script.targetPackage}")
                }
                
                val service = AutoAccessibilityService.getInstance()!!
                service.openApp(script.targetPackage)
                
                delay(2000)
                
                executeScriptConfig(service, script.config, recordId)
                
                completeTask(savedRecord, TaskStatus.SUCCESS)
                
            } catch (e: ExecutionException) {
                handleTaskError(savedRecord, e.errorCode, e.message ?: "执行失败")
                result = Result.failure(e)
            } catch (e: CancellationException) {
                completeTask(savedRecord, TaskStatus.INTERRUPTED)
                result = Result.failure(Exception("任务已中断"))
            } catch (e: Exception) {
                handleTaskError(savedRecord, ErrorCode.SCRIPT_CONFIG_ERROR, e.message ?: "未知错误")
                result = Result.failure(e)
            } finally {
                _isRunning.value = false
                _currentTask.value = null
                statusManager.setStatus("空闲")
            }
        }
        
        currentJob?.join()
        return result
    }
    
    private suspend fun executeScriptConfig(
        service: AutoAccessibilityService,
        config: ScriptConfig,
        recordId: Long
    ) {
        config.initControls?.forEach { controlConfig ->
            if (!_isRunning.value) return
            executeControl(service, controlConfig, recordId)
        }
        
        val loopConfig = config.loopConfig
        val iterations = loopConfig?.loopCount ?: 1
        val loopInterval = loopConfig?.loopInterval ?: 2000L
        
        repeat(iterations) { iteration ->
            if (!_isRunning.value) return
            
            if (loopConfig?.exitCondition != null) {
                if (checkExitCondition(service, loopConfig.exitCondition)) {
                    repository.insertLog(
                        ExecutionLog(
                            taskId = recordId,
                            logType = LogType.RESULT,
                            logContent = "满足退出条件，结束循环"
                        )
                    )
                    return
                }
            }
            
            config.conditions?.let { conditions ->
                executeConditions(service, conditions, config, recordId)
            } ?: run {
                config.controls?.forEach { controlConfig ->
                    executeControl(service, controlConfig, recordId)
                }
            }
            
            if (iteration < iterations - 1) {
                delay(loopInterval)
            }
        }
    }
    
    private suspend fun executeConditions(
        service: AutoAccessibilityService,
        conditions: List<ConditionConfig>,
        config: ScriptConfig,
        recordId: Long
    ) {
        for (condition in conditions) {
            if (!_isRunning.value) return
            
            val conditionMet = checkCondition(service, condition)
            val branch = if (conditionMet) condition.trueBranch else condition.falseBranch
            
            branch?.forEach { actionId ->
                config.controls?.find { it.controlId == actionId }?.let { controlConfig ->
                    executeControl(service, controlConfig, recordId)
                }
            }
        }
    }
    
    private suspend fun checkCondition(
        service: AutoAccessibilityService,
        condition: ConditionConfig
    ): Boolean {
        return when (condition.conditionType) {
            "control_exists" -> {
                val config = ControlConfig(
                    controlId = condition.conditionId,
                    controlName = "",
                    matchType = "text",
                    matchValue = condition.conditionValue,
                    operation = "check"
                )
                controlEngine.findControl(config) != null
            }
            "text_exists" -> {
                service.findNodeByText(condition.conditionValue) != null
            }
            else -> false
        }
    }
    
    private suspend fun checkExitCondition(
        service: AutoAccessibilityService,
        condition: ConditionConfig
    ): Boolean {
        return checkCondition(service, condition)
    }
    
    private suspend fun executeControl(
        service: AutoAccessibilityService,
        config: ControlConfig,
        recordId: Long
    ) {
        if (!_isRunning.value) return
        
        if (controlEngine.isSpecialOperation(config)) {
            repository.insertLog(
                ExecutionLog(
                    taskId = recordId,
                    logType = LogType.OPERATION,
                    logContent = "执行特殊操作: ${config.operation}",
                    detail = "控件: ${config.controlName}\n匹配类型: ${config.matchType}\n匹配值: ${config.matchValue}"
                )
            )
            
            val success = controlEngine.executeSpecialOperation(config)
            if (!success) {
                repository.insertLog(
                    ExecutionLog(
                        taskId = recordId,
                        logType = LogType.ERROR,
                        logContent = "特殊操作执行失败: ${config.controlName}",
                        detail = "操作类型: ${config.operation}\n匹配值: ${config.matchValue}"
                    )
                )
            }
            return
        }
        
        val retryCount = config.retryCount
        var lastException: Exception? = null
        var lastNodeInfo: String? = null
        
        repeat(retryCount) { attempt ->
            var node: AccessibilityNodeInfo? = null
            try {
                repository.insertLog(
                    ExecutionLog(
                        taskId = recordId,
                        logType = LogType.OPERATION,
                        logContent = "查找控件: ${config.controlName}",
                        detail = "匹配类型: ${config.matchType}\n匹配值: ${config.matchValue}\n匹配模式: ${config.matchMode}\n尝试: ${attempt + 1}/$retryCount"
                    )
                )
                
                node = controlEngine.findControl(config)
                
                if (node == null) {
                    val screenInfo = getCurrentScreenInfo(service)
                    lastNodeInfo = screenInfo
                    repository.insertLog(
                        ExecutionLog(
                            taskId = recordId,
                            logType = LogType.ERROR,
                            logContent = "未找到控件: ${config.controlName}",
                            detail = "匹配类型: ${config.matchType}\n匹配值: ${config.matchValue}\n尝试: ${attempt + 1}/$retryCount\n\n当前屏幕信息:\n$screenInfo"
                        )
                    )
                    throw ExecutionException(ErrorCode.CONTROL_NOT_FOUND, "控件未找到: ${config.controlName}")
                }
                
                val nodeBounds = controlEngine.getNodeBounds(node)
                val nodeText = controlEngine.getNodeText(node)
                
                repository.insertLog(
                    ExecutionLog(
                        taskId = recordId,
                        logType = LogType.OPERATION,
                        logContent = "执行操作: ${config.operation}",
                        detail = "控件: ${config.controlName}\n操作: ${config.operation}\n节点文本: ${nodeText}\n节点位置: (${nodeBounds.left}, ${nodeBounds.top}) - (${nodeBounds.right}, ${nodeBounds.bottom})\n可点击: ${controlEngine.isNodeClickable(node)}\n可见: ${controlEngine.isNodeVisible(node)}"
                    )
                )
                
                val params = mutableMapOf<String, Any>(
                    "delay_before" to config.delayBefore,
                    "delay_after" to config.delayAfter
                )
                
                config.inputText?.let { params["text"] = it }
                
                val success = controlEngine.performOperation(node, config.operation, params)
                
                if (!success) {
                    repository.insertLog(
                        ExecutionLog(
                            taskId = recordId,
                            logType = LogType.ERROR,
                            logContent = "操作执行失败: ${config.operation}",
                            detail = "控件: ${config.controlName}\n操作: ${config.operation}\n节点文本: ${nodeText}"
                        )
                    )
                    throw ExecutionException(ErrorCode.OPERATION_FAILED, "操作执行失败")
                }
                
                repository.insertLog(
                    ExecutionLog(
                        taskId = recordId,
                        logType = LogType.OPERATION,
                        logContent = "操作成功: ${config.controlName}",
                        detail = "操作: ${config.operation}"
                    )
                )
                
                return
                
            } catch (e: ExecutionException) {
                lastException = e
            } finally {
                node?.recycle()
            }
            delay(1000)
        }
        
        throw lastException ?: ExecutionException(ErrorCode.CONTROL_NOT_FOUND, "控件操作失败")
    }
    
    private fun getCurrentScreenInfo(service: AutoAccessibilityService): String {
        val root = service.getRootNode() ?: return "无法获取屏幕根节点"
        val sb = StringBuilder()
        
        fun collectNodeInfo(node: android.view.accessibility.AccessibilityNodeInfo, depth: Int) {
            val indent = "  ".repeat(depth)
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            val text = node.text?.toString()?.take(50) ?: ""
            val desc = node.contentDescription?.toString()?.take(30) ?: ""
            val className = node.className?.toString()?.substringAfterLast('.') ?: "?"
            
            sb.append("$indent$className: text=\"$text\" desc=\"$desc\" bounds=$bounds clickable=${node.isClickable}\n")
            
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { collectNodeInfo(it, depth + 1) }
            }
        }
        
        collectNodeInfo(root, 0)
        return sb.toString().take(2000)
    }
    
    private suspend fun handleAdIfNeeded(
        service: AutoAccessibilityService,
        config: ScriptConfig,
        recordId: Long
    ) {
        val adResult = adEngine.handleAd(service, config.adConfig)
        
        repository.insertLog(
            ExecutionLog(
                taskId = recordId,
                logType = LogType.AD_HANDLE,
                logContent = when (adResult) {
                    is AdHandleResult.NoAd -> "无广告"
                    is AdHandleResult.Closed -> "广告已关闭"
                    is AdHandleResult.Waited -> "等待广告: ${adResult.duration}秒"
                    is AdHandleResult.Failed -> "广告处理失败"
                }
            )
        )
    }
    
    private suspend fun completeTask(record: TaskRecord, status: Int) {
        val completedRecord = record.copy(
            endTime = System.currentTimeMillis(),
            status = status,
            duration = record.calculateDuration()
        )
        
        repository.updateRecord(completedRecord)
        
        repository.insertLog(
            ExecutionLog(
                taskId = record.id,
                logType = LogType.RESULT,
                logContent = "任务${if (status == TaskStatus.SUCCESS) "成功" else if (status == TaskStatus.INTERRUPTED) "已中断" else "失败"}完成",
                detail = "耗时: ${TimeUtils.formatDuration(completedRecord.duration)}"
            )
        )
        
        reportTaskToServer(completedRecord)
    }
    
    private suspend fun handleTaskError(record: TaskRecord, errorCode: String, errorMsg: String) {
        val failedRecord = record.copy(
            endTime = System.currentTimeMillis(),
            status = TaskStatus.FAILED,
            errorCode = errorCode,
            errorMsg = errorMsg,
            duration = record.calculateDuration()
        )
        
        repository.updateRecord(failedRecord)
        
        repository.insertLog(
            ExecutionLog(
                taskId = record.id,
                logType = LogType.ERROR,
                logContent = "任务失败: $errorMsg",
                detail = "错误码: $errorCode"
            )
        )
        
        reportTaskToServer(failedRecord)
    }
    
    private suspend fun reportTaskToServer(record: TaskRecord) {
        val result = repository.reportTask(record)
        
        result.fold(
            onSuccess = { serverRecordId ->
                val logs = repository.getLogsByTaskIdDirect(record.id)
                if (logs.isNotEmpty()) {
                    val serverLogs = logs.map { it.copy(taskId = serverRecordId) }
                    repository.reportLogs(serverLogs)
                }
            },
            onFailure = {
                Log.e(TAG, "Failed to report task: ${it.message}")
            }
        )
    }
    
    fun stopExecution() {
        currentJob?.cancel()
        currentJob = null
        _isRunning.value = false
        _currentTask.value = null
        statusManager.setStatus("空闲")
    }
    
    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

class ExecutionException(
    val errorCode: String,
    message: String
) : Exception(message)
