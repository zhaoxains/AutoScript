package com.auto.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.auto.app.R
import com.auto.app.data.repository.AppRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class ScreenCaptureService : Service() {
    
    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val CHANNEL_ID = "screen_capture_channel"
        private const val NOTIFICATION_ID = 1002
        
        private var mediaProjectionResultCode: Int = 0
        private var mediaProjectionData: Intent? = null
        private var onScreenshotCallback: ((Boolean) -> Unit)? = null
        
        fun setMediaProjectionResult(resultCode: Int, data: Intent) {
            mediaProjectionResultCode = resultCode
            mediaProjectionData = data
        }
        
        fun setOnScreenshotCallback(callback: (Boolean) -> Unit) {
            onScreenshotCallback = callback
        }
        
        fun hasPermission(): Boolean {
            return mediaProjectionData != null
        }
    }
    
    @Inject
    lateinit var repository: AppRepository
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var windowManager: WindowManager? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        val action = intent?.action
        when (action) {
            "CAPTURE" -> {
                captureScreen()
            }
            "STOP" -> {
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        releaseResources()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "屏幕截图服务",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("屏幕截图服务")
            .setContentText("正在运行")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun captureScreen() {
        if (mediaProjectionData == null) {
            Log.e(TAG, "No MediaProjection permission")
            onScreenshotCallback?.invoke(false)
            stopSelf()
            return
        }
        
        try {
            val metrics = DisplayMetrics()
            windowManager?.defaultDisplay?.getRealMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi
            
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(mediaProjectionResultCode, mediaProjectionData!!)
            
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                Handler(Looper.getMainLooper())
            )
            
            Handler(Looper.getMainLooper()).postDelayed({
                captureImage()
            }, 500)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture screen", e)
            onScreenshotCallback?.invoke(false)
            stopSelf()
        }
    }
    
    private fun captureImage() {
        try {
            val image = imageReader?.acquireLatestImage()
            if (image == null) {
                Log.e(TAG, "Failed to acquire image")
                onScreenshotCallback?.invoke(false)
                stopSelf()
                return
            }
            
            val bitmap = imageToBitmap(image)
            image.close()
            
            if (bitmap != null) {
                saveAndUploadScreenshot(bitmap)
            } else {
                Log.e(TAG, "Failed to convert image to bitmap")
                onScreenshotCallback?.invoke(false)
                stopSelf()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture image", e)
            onScreenshotCallback?.invoke(false)
            stopSelf()
        }
    }
    
    private fun imageToBitmap(image: Image): Bitmap? {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        
        if (rowPadding > 0) {
            val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            bitmap.recycle()
            return croppedBitmap
        }
        
        return bitmap
    }
    
    private fun saveAndUploadScreenshot(bitmap: Bitmap) {
        serviceScope.launch {
            try {
                val screenshotDir = File(cacheDir, "screenshots")
                if (!screenshotDir.exists()) {
                    screenshotDir.mkdirs()
                }
                
                val screenshotFile = File(screenshotDir, "screenshot_${System.currentTimeMillis()}.jpg")
                FileOutputStream(screenshotFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                bitmap.recycle()
                
                Log.d(TAG, "Screenshot saved: ${screenshotFile.absolutePath}, size: ${screenshotFile.length()}")
                
                val result = repository.uploadScreenshot(screenshotFile.absolutePath)
                result.fold(
                    onSuccess = { screenshotId ->
                        Log.d(TAG, "Screenshot uploaded successfully, id=$screenshotId")
                        screenshotFile.delete()
                        onScreenshotCallback?.invoke(true)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to upload screenshot", error)
                        screenshotFile.delete()
                        onScreenshotCallback?.invoke(false)
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save/upload screenshot", e)
                onScreenshotCallback?.invoke(false)
            } finally {
                stopSelf()
            }
        }
    }
    
    private fun releaseResources() {
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.surface?.release()
        imageReader?.close()
        imageReader = null
        
        mediaProjection?.stop()
        mediaProjection = null
    }
}
