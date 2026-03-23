package com.auto.app.engine

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.auto.app.data.model.AdConfig
import com.auto.app.service.AutoAccessibilityService
import com.auto.app.util.RandomUtils
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdEngine @Inject constructor(
    private val controlEngine: ControlEngine
) {
    
    private val closeKeywords = listOf("跳过", "关闭", "×", "X", "跳过广告", "关闭广告", "skip", "close")
    private val adKeywords = listOf("广告", "AD", "ad", "sponsored", "推广")
    private val durationPattern = Regex("(\\d+)\\s*[s秒]?")
    
    suspend fun detectAd(service: AutoAccessibilityService): AdDetectionResult {
        val root = service.getRootNode() ?: return AdDetectionResult.NoAd
        
        if (hasAdKeywords(root)) {
            val duration = extractAdDuration(root)
            val closeButton = findCloseButton(service, root)
            
            return if (closeButton != null) {
                AdDetectionResult.AdWithClose(duration, closeButton)
            } else {
                AdDetectionResult.AdWithDuration(duration)
            }
        }
        
        return AdDetectionResult.NoAd
    }
    
    private fun hasAdKeywords(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        
        if (adKeywords.any { text.contains(it, ignoreCase = true) || contentDesc.contains(it, ignoreCase = true) }) {
            return true
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                if (hasAdKeywords(it)) return true
            }
        }
        
        return false
    }
    
    private fun extractAdDuration(node: AccessibilityNodeInfo): Int? {
        val text = node.text?.toString() ?: ""
        
        durationPattern.find(text)?.let { match ->
            return match.groupValues[1].toIntOrNull()
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                extractAdDuration(child)?.let { return it }
            }
        }
        
        return null
    }
    
    private suspend fun findCloseButton(
        service: AutoAccessibilityService, 
        root: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        for (keyword in closeKeywords) {
            val nodes = root.findAccessibilityNodeInfosByText(keyword)
            val closeButton = nodes.find { isLikelyCloseButton(it) }
            if (closeButton != null) return closeButton
        }
        
        val cornerCloseButton = findCloseButtonInCorners(service, root)
        if (cornerCloseButton != null) return cornerCloseButton
        
        return null
    }
    
    private fun isLikelyCloseButton(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        
        val width = rect.width()
        val height = rect.height()
        
        if (width in 20..150 && height in 20..150) {
            return true
        }
        
        val text = node.text?.toString() ?: ""
        return closeKeywords.any { text.contains(it, ignoreCase = true) }
    }
    
    private fun findCloseButtonInCorners(
        service: AutoAccessibilityService, 
        root: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        val screenWidth = rootBounds.width()
        val screenHeight = rootBounds.height()
        
        val corners = listOf(
            Pair(0.9f, 0.1f),
            Pair(0.1f, 0.1f),
            Pair(0.9f, 0.9f),
            Pair(0.1f, 0.9f)
        )
        
        for ((xRatio, yRatio) in corners) {
            val x = (screenWidth * xRatio).toInt()
            val y = (screenHeight * yRatio).toInt()
            
            val node = findNodeAtPosition(root, x, y)
            if (node != null && isLikelyCloseButton(node)) {
                return node
            }
        }
        
        return null
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
    
    suspend fun handleAd(
        service: AutoAccessibilityService,
        config: AdConfig?,
        defaultWait: Int = 15
    ): AdHandleResult {
        val detection = detectAd(service)
        
        return when (detection) {
            is AdDetectionResult.NoAd -> AdHandleResult.NoAd
            is AdDetectionResult.AdWithDuration -> {
                val duration = detection.duration ?: config?.defaultWait ?: defaultWait
                delay(duration * 1000L)
                AdHandleResult.Waited(duration)
            }
            is AdDetectionResult.AdWithClose -> {
                val clicked = service.clickNode(detection.closeButton)
                delay(RandomUtils.randomDelay(500, 1000))
                
                if (clicked) {
                    AdHandleResult.Closed
                } else {
                    val duration = detection.duration ?: config?.defaultWait ?: defaultWait
                    delay(duration * 1000L)
                    AdHandleResult.Waited(duration)
                }
            }
        }
    }
    
    suspend fun waitForAdCompletion(
        service: AutoAccessibilityService,
        maxWaitSeconds: Int = 60,
        checkIntervalMs: Long = 1000
    ): Boolean {
        var waited = 0L
        
        while (waited < maxWaitSeconds * 1000) {
            val detection = detectAd(service)
            
            if (detection is AdDetectionResult.NoAd) {
                return true
            }
            
            delay(checkIntervalMs)
            waited += checkIntervalMs
        }
        
        return false
    }
}

sealed class AdDetectionResult {
    object NoAd : AdDetectionResult()
    data class AdWithDuration(val duration: Int?) : AdDetectionResult()
    data class AdWithClose(val duration: Int?, val closeButton: AccessibilityNodeInfo) : AdDetectionResult()
}

sealed class AdHandleResult {
    object NoAd : AdHandleResult()
    object Closed : AdHandleResult()
    data class Waited(val duration: Int) : AdHandleResult()
    object Failed : AdHandleResult()
}
