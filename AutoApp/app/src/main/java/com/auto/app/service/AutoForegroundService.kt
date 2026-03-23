package com.auto.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.auto.app.AutoApp
import com.auto.app.R
import com.auto.app.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AutoForegroundService : Service() {
    
    companion object {
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "com.auto.app.action.START"
        const val ACTION_STOP = "com.auto.app.action.STOP"
        const val ACTION_UPDATE_STATUS = "com.auto.app.action.UPDATE_STATUS"
        
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_TASK_NAME = "extra_task_name"
    }
    
    @Inject
    lateinit var statusManager: ExecutionStatusManager
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_STATUS -> {
                val status = intent.getStringExtra(EXTRA_STATUS) ?: "空闲"
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME)
                updateNotification(status, taskName)
            }
        }
        return START_STICKY
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, AutoApp.CHANNEL_ID_FOREGROUND)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("准备就绪")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(status: String, taskName: String?) {
        val notification = NotificationCompat.Builder(this, AutoApp.CHANNEL_ID_FOREGROUND)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(if (taskName != null) "$status: $taskName" else status)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}

class ExecutionStatusManager @Inject constructor() {
    private var currentStatus: String = "空闲"
    private var currentTaskName: String? = null
    private var statusListener: ((String, String?) -> Unit)? = null
    
    fun setStatus(status: String, taskName: String? = null) {
        currentStatus = status
        currentTaskName = taskName
        statusListener?.invoke(status, taskName)
    }
    
    fun getStatus(): String = currentStatus
    
    fun getTaskName(): String? = currentTaskName
    
    fun setStatusListener(listener: (String, String?) -> Unit) {
        statusListener = listener
    }
    
    fun clearStatusListener() {
        statusListener = null
    }
    
    fun reset() {
        currentStatus = "空闲"
        currentTaskName = null
        statusListener = null
    }
}
