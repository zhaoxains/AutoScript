package com.auto.app.data.remote.response

import com.auto.app.data.model.GlobalConfig
import com.auto.app.data.model.Script
import com.auto.app.data.model.User
import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("token")
    val token: String,
    
    @SerializedName("user_info")
    val userInfo: User
)

data class ScriptsResponse(
    @SerializedName("version")
    val version: String,
    
    @SerializedName("need_update")
    val needUpdate: Boolean,
    
    @SerializedName("scripts")
    val scripts: List<Script>,
    
    @SerializedName("global_config")
    val globalConfig: GlobalConfig
)

data class HeartbeatResponse(
    @SerializedName("server_time")
    val serverTime: String,
    
    @SerializedName("commands")
    val commands: List<RemoteCommand>
)

data class RemoteCommand(
    @SerializedName("command")
    val command: String,
    
    @SerializedName("params")
    val params: Map<String, Any>? = null
)

data class ReportResponse(
    @SerializedName("record_id")
    val recordId: Long
)

data class LogReportResponse(
    @SerializedName("success_count")
    val successCount: Int,
    
    @SerializedName("fail_count")
    val failCount: Int
)

data class ScreenshotUploadResponse(
    @SerializedName("screenshot_id")
    val screenshotId: Long,
    
    @SerializedName("image_path")
    val imagePath: String
)
