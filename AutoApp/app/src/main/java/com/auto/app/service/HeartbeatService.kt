package com.auto.app.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.auto.app.data.repository.AppRepository
import com.auto.app.engine.ExecutionEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class HeartbeatService @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AppRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val WORK_NAME = "heartbeat_work"
        
        fun start(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<HeartbeatService>(
                30, TimeUnit.SECONDS
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
        
        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val status = if (AutoAccessibilityService.isServiceEnabled()) {
                    if (ExecutionEngine.isRunning()) 2 else 1
                } else {
                    0
                }
                
                val currentTask = if (status == 2) {
                    inputData.getString("task_name")
                } else {
                    null
                }
                
                repository.heartbeat(status, currentTask)
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }
}
