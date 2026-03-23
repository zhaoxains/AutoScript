package com.auto.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "execution_logs",
    foreignKeys = [
        ForeignKey(
            entity = TaskRecord::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
@Parcelize
data class ExecutionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val taskId: Long,
    
    val logType: String,
    
    val logTime: Long = System.currentTimeMillis(),
    
    val logContent: String,
    
    val detail: String? = null
) : Parcelable

object LogType {
    const val TASK_INFO = "task_info"
    const val OPERATION = "operation"
    const val AD_HANDLE = "ad_handle"
    const val ERROR = "error"
    const val RESULT = "result"
    
    fun getLogTypeName(type: String): String {
        return when (type) {
            TASK_INFO -> "任务信息"
            OPERATION -> "操作步骤"
            AD_HANDLE -> "广告处理"
            ERROR -> "异常错误"
            RESULT -> "执行结果"
            else -> "其他"
        }
    }
}
