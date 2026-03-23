package com.auto.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "task_records")
@Parcelize
data class TaskRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val userId: Int,
    
    val deviceId: String,
    
    val scriptId: Int,
    
    val taskName: String,
    
    val startTime: Long,
    
    var endTime: Long? = null,
    
    var status: Int = TaskStatus.PENDING,
    
    var duration: Long = 0,
    
    var errorCode: String? = null,
    
    var errorMsg: String? = null,
    
    var taskExtend: String? = null,
    
    val createTime: Long = System.currentTimeMillis()
) : Parcelable {
    
    fun isSuccess(): Boolean = status == TaskStatus.SUCCESS
    
    fun isFailed(): Boolean = status == TaskStatus.FAILED
    
    fun isRunning(): Boolean = status == TaskStatus.RUNNING
    
    fun calculateDuration(): Long {
        return if (endTime != null) {
            (endTime!! - startTime) / 1000
        } else {
            0
        }
    }
}

object TaskStatus {
    const val PENDING = 0
    const val RUNNING = 1
    const val SUCCESS = 2
    const val FAILED = 3
    const val INTERRUPTED = 4
    
    fun getStatusName(status: Int): String {
        return when (status) {
            PENDING -> "待执行"
            RUNNING -> "执行中"
            SUCCESS -> "执行成功"
            FAILED -> "执行失败"
            INTERRUPTED -> "已中断"
            else -> "未知"
        }
    }
}

object ErrorCode {
    const val CONTROL_NOT_FOUND = "E001"
    const val AD_CLOSE_FAILED = "E002"
    const val APP_LOAD_TIMEOUT = "E003"
    const val APP_NOT_INSTALLED = "E004"
    const val PERMISSION_DENIED = "E005"
    const val NETWORK_ERROR = "E006"
    const val SCRIPT_CONFIG_ERROR = "E007"
    const val TASK_TIMEOUT = "E008"
    const val OPERATION_FAILED = "E009"
    const val APP_NOT_FOUND = "E010"
    
    fun getErrorMsg(code: String): String {
        return when (code) {
            CONTROL_NOT_FOUND -> "控件识别失败"
            AD_CLOSE_FAILED -> "广告关闭失败"
            APP_LOAD_TIMEOUT -> "APP加载超时"
            APP_NOT_INSTALLED -> "APP未安装"
            PERMISSION_DENIED -> "权限被拒绝"
            NETWORK_ERROR -> "网络异常"
            SCRIPT_CONFIG_ERROR -> "脚本配置错误"
            TASK_TIMEOUT -> "任务执行超时"
            OPERATION_FAILED -> "操作执行失败"
            APP_NOT_FOUND -> "APP未找到"
            else -> "未知错误"
        }
    }
}
