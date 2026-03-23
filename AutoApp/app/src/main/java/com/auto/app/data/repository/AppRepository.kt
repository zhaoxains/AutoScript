package com.auto.app.data.repository

import android.content.Context
import com.auto.app.data.local.db.AppDatabase
import com.auto.app.data.local.db.ExecutionLogDao
import com.auto.app.data.local.db.TaskRecordDao
import com.auto.app.data.local.prefs.PreferencesManager
import com.auto.app.data.model.ExecutionLog
import com.auto.app.data.model.TaskRecord
import com.auto.app.data.model.GlobalConfig
import com.auto.app.data.model.Script
import com.auto.app.data.model.User
import com.auto.app.data.remote.ApiService
import com.auto.app.data.remote.request.HeartbeatRequest
import com.auto.app.data.remote.request.LoginRequest
import com.auto.app.data.remote.request.LogItem
import com.auto.app.data.remote.request.LogReportRequest
import com.auto.app.data.remote.request.RegisterRequest
import com.auto.app.data.remote.request.TaskReportRequest
import com.auto.app.util.DeviceUtils
import com.auto.app.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val apiService: ApiService,
    private val database: AppDatabase,
    private val preferencesManager: PreferencesManager,
    private val context: Context
) {
    private val taskRecordDao: TaskRecordDao = database.taskRecordDao()
    private val executionLogDao: ExecutionLogDao = database.executionLogDao()
    
    suspend fun login(username: String, password: String, deviceId: String): Result<User> {
        return try {
            val request = LoginRequest(username, password, deviceId)
            val response = apiService.login(request)
            
            if (response.isSuccess() && response.data != null) {
                preferencesManager.saveToken(response.data.token)
                preferencesManager.saveUser(response.data.userInfo)
                preferencesManager.saveDeviceId(deviceId)
                preferencesManager.setLoggedIn(true)
                Result.success(response.data.userInfo)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun register(username: String, password: String, referrer: String = ""): Result<User> {
        return try {
            val deviceId = getDeviceId() ?: DeviceUtils.getDeviceId(context)
            saveDeviceId(deviceId)
            val request = RegisterRequest(username, password, password, deviceId, referrer)
            val response = apiService.register(request)
            
            if (response.isSuccess() && response.data != null) {
                preferencesManager.saveToken(response.data.token)
                preferencesManager.saveUser(response.data.userInfo)
                preferencesManager.saveDeviceId(deviceId)
                preferencesManager.setLoggedIn(true)
                Result.success(response.data.userInfo)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun logout(): Result<Unit> {
        return try {
            apiService.logout()
            preferencesManager.clearLoginInfo()
            Result.success(Unit)
        } catch (e: Exception) {
            preferencesManager.clearLoginInfo()
            Result.success(Unit)
        }
    }
    
    suspend fun fetchScripts(): Result<Pair<List<Script>, GlobalConfig>> {
        return try {
            val localVersion = preferencesManager.getScriptsVersion()
            val response = apiService.getScripts(localVersion)
            
            if (response.isSuccess() && response.data != null) {
                if (response.data.needUpdate) {
                    preferencesManager.saveScripts(response.data.scripts)
                    preferencesManager.saveScriptsVersion(response.data.version)
                }
                response.data.globalConfig?.let {
                    preferencesManager.saveGlobalConfig(it)
                }
                Result.success(Pair(response.data.scripts, response.data.globalConfig))
            } else {
                val localScripts = preferencesManager.getScripts()
                val localConfig = preferencesManager.getGlobalConfig()
                if (localScripts.isNotEmpty()) {
                    Result.success(Pair(localScripts, localConfig ?: GlobalConfig()))
                } else {
                    Result.failure(Exception(response.msg))
                }
            }
        } catch (e: Exception) {
            val localScripts = preferencesManager.getScripts()
            val localConfig = preferencesManager.getGlobalConfig()
            if (localScripts.isNotEmpty()) {
                Result.success(Pair(localScripts, localConfig ?: GlobalConfig()))
            } else {
                Result.failure(e)
            }
        }
    }
    
    suspend fun heartbeat(status: Int, currentTask: String? = null): Result<Unit> {
        return try {
            val deviceId = preferencesManager.getDeviceId() ?: return Result.failure(Exception("Device ID not found"))
            val request = HeartbeatRequest(deviceId, status, currentTask)
            val response = apiService.heartbeat(request)
            
            if (response.isSuccess()) {
                preferencesManager.saveLastHeartbeat(System.currentTimeMillis())
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendHeartbeat(deviceId: String, status: Int, currentTask: String?): com.auto.app.data.remote.response.HeartbeatResponse? {
        return try {
            val request = HeartbeatRequest(deviceId, status, currentTask)
            val response = apiService.heartbeat(request)
            
            if (response.isSuccess()) {
                preferencesManager.saveLastHeartbeat(System.currentTimeMillis())
                response.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun reportTask(record: TaskRecord): Result<Long> {
        return try {
            val request = TaskReportRequest(
                scriptId = record.scriptId,
                taskName = record.taskName,
                startTime = TimeUtils.formatTime(record.startTime),
                endTime = record.endTime?.let { TimeUtils.formatTime(it) },
                status = record.status,
                duration = record.duration,
                errorCode = record.errorCode,
                errorMsg = record.errorMsg,
                taskExtend = record.taskExtend
            )
            
            val response = apiService.reportTask(request)
            if (response.isSuccess() && response.data != null) {
                Result.success(response.data.recordId)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun reportLogs(logs: List<ExecutionLog>): Result<Pair<Int, Int>> {
        return try {
            val request = LogReportRequest(
                logs = logs.map {
                    LogItem(
                        taskId = it.taskId,
                        logType = it.logType,
                        logTime = TimeUtils.formatTime(it.logTime),
                        logContent = it.logContent,
                        detail = it.detail
                    )
                }
            )
            
            val response = apiService.reportLogs(request)
            if (response.isSuccess() && response.data != null) {
                Result.success(Pair(response.data.successCount, response.data.failCount))
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getUser(): User? = preferencesManager.getUser()
    
    fun getToken(): String? = preferencesManager.getToken()
    
    fun getDeviceId(): String? = preferencesManager.getDeviceId()
    
    fun saveDeviceId(deviceId: String) = preferencesManager.saveDeviceId(deviceId)
    
    fun isLoggedIn(): Boolean = preferencesManager.isLoggedIn()
    
    fun getScripts(): List<Script> = preferencesManager.getScripts()
    
    fun getGlobalConfig(): GlobalConfig? = preferencesManager.getGlobalConfig()
    
    fun getAllRecords(): Flow<List<TaskRecord>> = taskRecordDao.getAllRecords()
    
    fun getRecordsByStatus(status: Int): Flow<List<TaskRecord>> = taskRecordDao.getRecordsByStatus(status)
    
    fun getRecordsByScriptId(scriptId: Int): Flow<List<TaskRecord>> = taskRecordDao.getRecordsByScriptId(scriptId)
    
    suspend fun getRecordById(id: Long): TaskRecord? = taskRecordDao.getRecordById(id)
    
    suspend fun insertRecord(record: TaskRecord): Long = taskRecordDao.insertRecord(record)
    
    suspend fun updateRecord(record: TaskRecord) = taskRecordDao.updateRecord(record)
    
    suspend fun deleteRecord(record: TaskRecord) = taskRecordDao.deleteRecord(record)
    
    suspend fun countTodayRecords(): Int = taskRecordDao.countTodayRecords()
    
    fun getLogsByTaskId(taskId: Long): Flow<List<ExecutionLog>> = executionLogDao.getLogsByTaskId(taskId)
    
    suspend fun getLogsByTaskIdDirect(taskId: Long): List<ExecutionLog> = executionLogDao.getLogsByTaskIdDirect(taskId)
    
    suspend fun insertLog(log: ExecutionLog): Long = executionLogDao.insertLog(log)
    
    suspend fun insertLogs(logs: List<ExecutionLog>) = executionLogDao.insertLogs(logs)
    
    suspend fun deleteOldRecords(beforeTime: Long): Int = taskRecordDao.deleteOldRecords(beforeTime)
    
    suspend fun deleteOldLogs(beforeTime: Long): Int = executionLogDao.deleteOldLogs(beforeTime)
    
    suspend fun uploadScreenshot(imagePath: String): Result<Long> {
        return try {
            val file = File(imagePath)
            if (!file.exists()) {
                return Result.failure(Exception("Screenshot file not found"))
            }
            
            val requestFile = file.asRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("screenshot", file.name, requestFile)
            
            val response = apiService.uploadScreenshot(part)
            
            if (response.isSuccess() && response.data != null) {
                Result.success(response.data.screenshotId)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
