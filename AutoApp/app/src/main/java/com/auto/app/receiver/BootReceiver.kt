package com.auto.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.auto.app.service.AutoForegroundService

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, AutoForegroundService::class.java).apply {
                action = AutoForegroundService.ACTION_START
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
