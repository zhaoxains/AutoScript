package com.auto.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AutoApp : Application() {
    
    companion object {
        const val CHANNEL_ID_FOREGROUND = "auto_foreground"
        const val CHANNEL_ID_STATUS = "auto_status"
        const val CHANNEL_ID_ALERT = "auto_alert"
        
        @Volatile
        private var instance: AutoApp? = null
        
        fun getInstance(): AutoApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        MMKV.initialize(this)
        
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            val foregroundChannel = NotificationChannel(
                CHANNEL_ID_FOREGROUND,
                "前台服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "自动化执行服务状态"
                setShowBadge(false)
            }
            
            val statusChannel = NotificationChannel(
                CHANNEL_ID_STATUS,
                "执行状态",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "任务执行状态通知"
            }
            
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERT,
                "异常提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "异常和错误提醒"
                enableVibration(true)
            }
            
            notificationManager.createNotificationChannels(
                listOf(foregroundChannel, statusChannel, alertChannel)
            )
        }
    }
}
