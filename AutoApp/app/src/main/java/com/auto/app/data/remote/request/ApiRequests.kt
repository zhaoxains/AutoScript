package com.auto.app.data.remote.request

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username")
    val username: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("device_id")
    val deviceId: String
)

data class RegisterRequest(
    @SerializedName("username")
    val username: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("confirm_password")
    val confirmPassword: String,
    
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("referrer")
    val referrer: String = ""
)

data class HeartbeatRequest(
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("status")
    val status: Int,
    
    @SerializedName("current_task")
    val currentTask: String? = null
)

data class TaskReportRequest(
    @SerializedName("script_id")
    val scriptId: Int,
    
    @SerializedName("task_name")
    val taskName: String,
    
    @SerializedName("start_time")
    val startTime: String,
    
    @SerializedName("end_time")
    val endTime: String? = null,
    
    @SerializedName("status")
    val status: Int,
    
    @SerializedName("duration")
    val duration: Long = 0,
    
    @SerializedName("error_code")
    val errorCode: String? = null,
    
    @SerializedName("error_msg")
    val errorMsg: String? = null,
    
    @SerializedName("task_extend")
    val taskExtend: String? = null
)

data class LogReportRequest(
    @SerializedName("logs")
    val logs: List<LogItem>
)

data class LogItem(
    @SerializedName("task_id")
    val taskId: Long? = null,
    
    @SerializedName("log_type")
    val logType: String,
    
    @SerializedName("log_time")
    val logTime: String,
    
    @SerializedName("log_content")
    val logContent: String,
    
    @SerializedName("detail")
    val detail: String? = null
)
