package com.auto.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.auto.app.util.RandomUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AutoAccessibilityService : AccessibilityService() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    companion object {
        @Volatile
        private var instance: AutoAccessibilityService? = null
        
        fun getInstance(): AutoAccessibilityService? = instance
        
        fun isServiceEnabled(): Boolean = instance != null
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event handling will be implemented in script executor
    }
    
    override fun onInterrupt() {
        // Handle interruption
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
    
    fun findNodeByText(text: String, exact: Boolean = true): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        
        val nodes = if (exact) {
            root.findAccessibilityNodeInfosByText(text)
        } else {
            root.findAccessibilityNodeInfosByText(text).filter { 
                it.text?.toString()?.contains(text, ignoreCase = true) == true 
            }
        }
        
        return nodes.firstOrNull()
    }
    
    fun findNodeById(id: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val nodes = root.findAccessibilityNodeInfosByViewId(id)
        return nodes.firstOrNull()
    }
    
    fun findNodeByDescription(description: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val nodes = root.findAccessibilityNodeInfosByText(description)
        return nodes.firstOrNull { 
            it.contentDescription?.toString()?.contains(description, ignoreCase = true) == true 
        }
    }
    
    fun findNodeByClassName(className: String): List<AccessibilityNodeInfo> {
        val root = rootInActiveWindow ?: return emptyList()
        val result = mutableListOf<AccessibilityNodeInfo>()
        findNodesByClassName(root, className, result)
        return result
    }
    
    fun <T> useNode(node: AccessibilityNodeInfo?, block: (AccessibilityNodeInfo) -> T): T? {
        return try {
            node?.let { block(it) }
        } finally {
            node?.recycle()
        }
    }
    
    fun <T> useNodes(nodes: List<AccessibilityNodeInfo>, block: (List<AccessibilityNodeInfo>) -> T): T {
        return try {
            block(nodes)
        } finally {
            nodes.forEach { it.recycle() }
        }
    }
    
    private fun findNodesByClassName(
        node: AccessibilityNodeInfo, 
        className: String, 
        result: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.className?.toString()?.contains(className) == true) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findNodesByClassName(it, className, result) }
        }
    }
    
    fun findNodeByPosition(xRatio: Float, yRatio: Float): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        val screenWidth = rootBounds.width()
        val screenHeight = rootBounds.height()
        val targetX = (screenWidth * xRatio).toInt()
        val targetY = (screenHeight * yRatio).toInt()
        
        return findNodeAtPosition(root, targetX, targetY)
    }
    
    private fun findNodeAtPosition(node: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        
        if (rect.contains(x, y)) {
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    findNodeAtPosition(child, x, y)?.let { return it }
                }
            }
            return node
        }
        return null
    }
    
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
            }
            val rect = Rect()
            node.getBoundsInScreen(rect)
            clickAtPosition(rect.centerX(), rect.centerY())
        }
    }
    
    fun clickAtPosition(x: Int, y: Int): Boolean {
        val (offsetX, offsetY) = RandomUtils.randomCoordinate(x, y, 10)
        return performGesture(offsetX.toFloat(), offsetY.toFloat())
    }
    
    fun longClickNode(node: AccessibilityNodeInfo, durationMs: Long = 500): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return longClickAtPosition(rect.centerX(), rect.centerY(), durationMs)
    }
    
    fun longClickAtPosition(x: Int, y: Int, durationMs: Long = 500): Boolean {
        return performLongPress(x.toFloat(), y.toFloat(), durationMs)
    }
    
    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 500): Boolean {
        val path = Path()
        val curvePoints = RandomUtils.generateBesselCurve(startX, startY, endX, endY, 20)
        
        curvePoints.firstOrNull()?.let { path.moveTo(it.first, it.second) }
        curvePoints.drop(1).forEach { path.lineTo(it.first, it.second) }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        
        return dispatchGesture(gesture, null, null)
    }
    
    fun scroll(direction: ScrollDirection, distance: Float = 0.5f): Boolean {
        val root = rootInActiveWindow ?: return false
        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        val screenWidth = rootBounds.width().toFloat()
        val screenHeight = rootBounds.height().toFloat()
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        return when (direction) {
            ScrollDirection.UP -> swipe(centerX, centerY + screenHeight * distance, centerX, centerY - screenHeight * distance, 300)
            ScrollDirection.DOWN -> swipe(centerX, centerY - screenHeight * distance, centerX, centerY + screenHeight * distance, 300)
            ScrollDirection.LEFT -> swipe(centerX + screenWidth * distance, centerY, centerX - screenWidth * distance, centerY, 300)
            ScrollDirection.RIGHT -> swipe(centerX - screenWidth * distance, centerY, centerX + screenWidth * distance, centerY, 300)
        }
    }
    
    fun inputText(node: AccessibilityNodeInfo, text: String): Boolean {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }
    
    private fun performGesture(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        
        return dispatchGesture(gesture, null, null)
    }
    
    private fun performLongPress(x: Float, y: Float, durationMs: Long): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        
        return dispatchGesture(gesture, null, null)
    }
    
    suspend fun performGestureWithCallback(
        startX: Float, startY: Float, 
        endX: Float, endY: Float, 
        durationMs: Long = 500
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val path = Path()
        val curvePoints = RandomUtils.generateBesselCurve(startX, startY, endX, endY, 20)
        
        curvePoints.firstOrNull()?.let { path.moveTo(it.first, it.second) }
        curvePoints.drop(1).forEach { path.lineTo(it.first, it.second) }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        
        val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                continuation.resume(true)
            }
            
            override fun onCancelled(gestureDescription: GestureDescription?) {
                continuation.resume(false)
            }
        }
        
        dispatchGesture(gesture, callback, null)
    }
    
    fun openApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
    }
    
    fun goBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }
    
    fun goHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }
    
    fun openRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
    
    fun openNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }
    
    fun openQuickSettings(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }
    
    fun getRootNode(): AccessibilityNodeInfo? = rootInActiveWindow
    
    enum class ScrollDirection {
        UP, DOWN, LEFT, RIGHT
    }
}
