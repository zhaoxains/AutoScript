package com.auto.app.engine

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.auto.app.data.model.*
import com.auto.app.service.AutoAccessibilityService
import com.auto.app.util.RandomUtils
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControlEngine @Inject constructor() {
    
    companion object {
        private const val TAG = "ControlEngine"
    }
    
    suspend fun findControl(config: ControlConfig): AccessibilityNodeInfo? {
        val service = AutoAccessibilityService.getInstance() ?: return null
        
        return when (config.matchType) {
            "text" -> findNodeByTextMultiLanguage(service, config)
            "id" -> service.findNodeById(config.matchValue)
            "description" -> service.findNodeByDescription(config.matchValue)
            "class" -> service.findNodeByClassName(config.matchValue).firstOrNull()
            "icon" -> findIconNode(service, config)
            "position" -> null
            "delay", "gesture" -> null
            else -> null
        }
    }
    
    private fun findNodeByTextMultiLanguage(
        service: AutoAccessibilityService,
        config: ControlConfig
    ): AccessibilityNodeInfo? {
        val matchValues = config.multiLanguageValues ?: listOf(config.matchValue)
        
        for (value in matchValues) {
            val node = findNodeByText(service, value, config.matchMode)
            if (node != null) {
                Log.d(TAG, "Found node with multi-language value: $value")
                return node
            }
        }
        
        return null
    }
    
    fun isSpecialOperation(config: ControlConfig): Boolean {
        return config.matchType in listOf("delay", "gesture", "position", "icon") || 
               config.operation in listOf("wait", "wait_for_element", "swipe_up", "swipe_down", 
                                          "swipe_left", "swipe_right", "back", "home", "recents",
                                          "popup_skip", "batch_operation", "click_position")
    }
    
    suspend fun executeSpecialOperation(config: ControlConfig): Boolean {
        val service = AutoAccessibilityService.getInstance() ?: return false
        
        val delayBefore = config.delayBefore
        if (delayBefore > 0) delay(delayBefore)
        
        val result = when {
            config.matchType == "delay" || config.operation == "wait" -> {
                val waitTime = config.waitTime ?: 1000L
                delay(waitTime)
                true
            }
            config.operation == "wait_for_element" -> {
                executeWaitForElement(service, config)
            }
            config.operation == "back" -> {
                service.goBack()
            }
            config.operation == "home" -> {
                service.goHome()
            }
            config.operation == "recents" -> {
                service.openRecents()
            }
            config.operation == "popup_skip" -> {
                executePopupSkip(service, config)
            }
            config.operation == "batch_operation" -> {
                executeBatchOperation(service, config)
            }
            config.matchType == "position" || config.operation == "click_position" -> {
                executePositionClick(service, config)
            }
            config.matchType == "icon" -> {
                executeIconClick(service, config)
            }
            config.matchType == "gesture" || config.operation.startsWith("swipe") -> {
                executeSwipe(service, config)
            }
            else -> false
        }
        
        val delayAfter = config.delayAfter
        if (delayAfter > 0) delay(delayAfter)
        
        return result
    }
    
    private suspend fun executeWaitForElement(service: AutoAccessibilityService, config: ControlConfig): Boolean {
        val timeout = config.timeout ?: 30000L
        val checkInterval = config.checkInterval ?: 1000L
        val startTime = System.currentTimeMillis()
        
        Log.d(TAG, "executeWaitForElement: ${config.controlName}, timeout: $timeout")
        
        while (System.currentTimeMillis() - startTime < timeout) {
            val position = config.position ?: return false
            val root = service.getRootNode() ?: continue
            
            val rootBounds = android.graphics.Rect()
            root.getBoundsInScreen(rootBounds)
            val screenWidth = rootBounds.width()
            val screenHeight = rootBounds.height()
            
            val x = (screenWidth * position.xRatio).toInt()
            val y = (screenHeight * position.yRatio).toInt()
            
            val nodes = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
            collectNodesAtPosition(root, x, y, screenWidth, screenHeight, nodes)
            
            val hasClickableNode = nodes.any { it.isClickable || !it.contentDescription.isNullOrEmpty() }
            
            nodes.forEach { it.recycle() }
            root.recycle()
            
            if (hasClickableNode) {
                Log.d(TAG, "Element found at position: (${position.xRatio}, ${position.yRatio})")
                return true
            }
            
            delay(checkInterval)
        }
        
        Log.w(TAG, "Wait for element timeout: ${config.controlName}")
        return false
    }
    
    private fun collectNodesAtPosition(
        node: android.view.accessibility.AccessibilityNodeInfo,
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int,
        result: MutableList<android.view.accessibility.AccessibilityNodeInfo>
    ) {
        try {
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            
            if (bounds.contains(x, y)) {
                if (node.isClickable || !node.contentDescription.isNullOrEmpty() || !node.text.isNullOrEmpty()) {
                    result.add(node)
                }
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                collectNodesAtPosition(child, x, y, screenWidth, screenHeight, result)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun executeIconClick(service: AutoAccessibilityService, config: ControlConfig): Boolean {
        Log.d(TAG, "executeIconClick: ${config.controlName}")
        
        // 尝试通过节点属性定位图标
        val iconNode = findIconNode(service, config)
        if (iconNode != null) {
            Log.d(TAG, "Found icon node: ${iconNode.className}")
            return service.clickNode(iconNode)
        }
        
        // 尝试 fallback 策略
        config.fallbackStrategies?.forEach { strategy ->
            Log.d(TAG, "Trying fallback strategy: ${strategy.strategyType}")
            val success = executeFallbackStrategy(service, strategy, config)
            if (success) {
                Log.d(TAG, "Fallback strategy succeeded")
                return true
            }
        }
        
        Log.w(TAG, "Icon click failed: ${config.controlName}")
        return false
    }
    
    private fun findIconNode(service: AutoAccessibilityService, config: ControlConfig): AccessibilityNodeInfo? {
        val iconConfig = config.iconConfig ?: return null
        val root = service.getRootNode() ?: return null
        
        Log.d(TAG, "Finding icon node with config: $iconConfig")
        
        // 收集所有可能的候选节点
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectCandidateNodes(root, iconConfig, candidates)
        
        Log.d(TAG, "Found ${candidates.size} candidate nodes")
        
        // 根据配置筛选最佳匹配
        return candidates.firstOrNull { node ->
            matchIconNode(node, iconConfig, service)
        }
    }
    
    private fun collectCandidateNodes(
        node: AccessibilityNodeInfo,
        iconConfig: IconConfig,
        candidates: MutableList<AccessibilityNodeInfo>
    ) {
        // 检查节点是否匹配
        if (matchesIconConfig(node, iconConfig)) {
            candidates.add(node)
        }
        
        // 递归遍历子节点
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectCandidateNodes(child, iconConfig, candidates)
            }
        }
    }
    
    private fun matchesIconConfig(node: AccessibilityNodeInfo, iconConfig: IconConfig): Boolean {
        // 检查 contentDescription
        if (!iconConfig.contentDescription.isNullOrEmpty()) {
            val desc = node.contentDescription?.toString() ?: ""
            if (!desc.contains(iconConfig.contentDescription, ignoreCase = true)) {
                return false
            }
        }
        
        // 检查 viewId
        if (!iconConfig.viewId.isNullOrEmpty()) {
            val viewId = node.viewIdResourceName ?: ""
            if (!viewId.contains(iconConfig.viewId, ignoreCase = true)) {
                return false
            }
        }
        
        // 检查 className
        if (!iconConfig.className.isNullOrEmpty()) {
            val className = node.className?.toString() ?: ""
            if (!className.contains(iconConfig.className, ignoreCase = true)) {
                return false
            }
        }
        
        // 检查 clickable
        if (iconConfig.clickable != null && node.isClickable != iconConfig.clickable) {
            return false
        }
        
        return true
    }
    
    private fun matchIconNode(
        node: AccessibilityNodeInfo,
        iconConfig: IconConfig,
        service: AutoAccessibilityService
    ): Boolean {
        // 检查 bounds_hint
        iconConfig.boundsHint?.let { hint ->
            val root = service.getRootNode() ?: return false
            val rootBounds = Rect()
            root.getBoundsInScreen(rootBounds)
            val screenWidth = rootBounds.width()
            val screenHeight = rootBounds.height()
            
            val nodeBounds = Rect()
            node.getBoundsInScreen(nodeBounds)
            
            val xRatio = nodeBounds.centerX().toFloat() / screenWidth
            val yRatio = nodeBounds.centerY().toFloat() / screenHeight
            
            if (xRatio < hint.xRatioMin || xRatio > hint.xRatioMax ||
                yRatio < hint.yRatioMin || yRatio > hint.yRatioMax) {
                return false
            }
        }
        
        // 检查 parent_text
        if (!iconConfig.parentText.isNullOrEmpty()) {
            val parent = node.parent
            val parentText = parent?.text?.toString() ?: parent?.contentDescription?.toString() ?: ""
            if (!parentText.contains(iconConfig.parentText, ignoreCase = true)) {
                return false
            }
        }
        
        // 检查 sibling_text
        if (!iconConfig.siblingText.isNullOrEmpty()) {
            val parent = node.parent ?: return false
            var hasSibling = false
            for (i in 0 until parent.childCount) {
                val sibling = parent.getChild(i) ?: continue
                val siblingText = sibling.text?.toString() ?: sibling.contentDescription?.toString() ?: ""
                if (siblingText.contains(iconConfig.siblingText, ignoreCase = true)) {
                    hasSibling = true
                    break
                }
            }
            if (!hasSibling) return false
        }
        
        // 检查 position_hint (top_left, top_right, bottom_left, bottom_right)
        if (!iconConfig.positionHint.isNullOrEmpty()) {
            val root = service.getRootNode() ?: return false
            val rootBounds = Rect()
            root.getBoundsInScreen(rootBounds)
            
            val nodeBounds = Rect()
            node.getBoundsInScreen(nodeBounds)
            
            val isInPosition = when (iconConfig.positionHint) {
                "top_left" -> nodeBounds.centerX() < rootBounds.width() / 2 && nodeBounds.centerY() < rootBounds.height() / 2
                "top_right" -> nodeBounds.centerX() > rootBounds.width() / 2 && nodeBounds.centerY() < rootBounds.height() / 2
                "bottom_left" -> nodeBounds.centerX() < rootBounds.width() / 2 && nodeBounds.centerY() > rootBounds.height() / 2
                "bottom_right" -> nodeBounds.centerX() > rootBounds.width() / 2 && nodeBounds.centerY() > rootBounds.height() / 2
                else -> true
            }
            if (!isInPosition) return false
        }
        
        return true
    }
    
    private suspend fun executeFallbackStrategy(
        service: AutoAccessibilityService,
        strategy: FallbackStrategy,
        config: ControlConfig
    ): Boolean {
        return when (strategy.strategyType) {
            "text" -> {
                val node = findNodeByText(service, strategy.strategyValue, "contains")
                node?.let { service.clickNode(it) } ?: false
            }
            "description" -> {
                val node = service.findNodeByDescription(strategy.strategyValue)
                node?.let { service.clickNode(it) } ?: false
            }
            "id" -> {
                val node = service.findNodeById(strategy.strategyValue)
                node?.let { service.clickNode(it) } ?: false
            }
            "position" -> {
                strategy.position?.let { pos ->
                    val root = service.getRootNode() ?: return false
                    val rootBounds = Rect()
                    root.getBoundsInScreen(rootBounds)
                    val x = (rootBounds.width() * pos.xRatio).toInt()
                    val y = (rootBounds.height() * pos.yRatio).toInt()
                    service.clickAtPosition(x, y)
                } ?: false
            }
            "relative" -> {
                // 基于参考文本的相对位置点击
                strategy.position?.let { pos ->
                    if (!pos.relativeTo.isNullOrEmpty()) {
                        val refNode = findNodeByText(service, pos.relativeTo, "contains")
                        if (refNode != null) {
                            val root = service.getRootNode() ?: return false
                            val rootBounds = Rect()
                            root.getBoundsInScreen(rootBounds)
                            val refBounds = Rect()
                            refNode.getBoundsInScreen(refBounds)
                            
                            val x = refBounds.centerX() + (rootBounds.width() * pos.offsetXRatio).toInt()
                            val y = refBounds.centerY() + (rootBounds.height() * pos.offsetYRatio).toInt()
                            service.clickAtPosition(x, y)
                        } else false
                    } else false
                } ?: false
            }
            else -> false
        }
    }
    
    private fun executePositionClick(service: AutoAccessibilityService, config: ControlConfig): Boolean {
        val position = config.position ?: return false
        val root = service.getRootNode() ?: return false
        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        val screenWidth = rootBounds.width()
        val screenHeight = rootBounds.height()
        
        var x = (screenWidth * position.xRatio).toInt()
        var y = (screenHeight * position.yRatio).toInt()
        
        if (!position.relativeTo.isNullOrEmpty()) {
            val refNode = findNodeByText(service, position.relativeTo, "contains")
            if (refNode != null) {
                val refBounds = Rect()
                refNode.getBoundsInScreen(refBounds)
                x = refBounds.centerX() + (screenWidth * position.offsetXRatio).toInt()
                y = refBounds.centerY() + (screenHeight * position.offsetYRatio).toInt()
            }
        }
        
        return service.clickAtPosition(x, y)
    }
    
    private suspend fun executeSwipe(service: AutoAccessibilityService, config: ControlConfig): Boolean {
        val swipeConfig = config.swipeConfig
        
        val root = service.getRootNode() ?: return false
        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        val screenWidth = rootBounds.width().toFloat()
        val screenHeight = rootBounds.height().toFloat()
        
        return when (config.operation) {
            "swipe_up" -> {
                val startX = (swipeConfig?.startXRatio ?: 0.5f) * screenWidth
                val startY = (swipeConfig?.startYRatio ?: 0.7f) * screenHeight
                val endX = (swipeConfig?.endXRatio ?: 0.5f) * screenWidth
                val endY = (swipeConfig?.endYRatio ?: 0.3f) * screenHeight
                val duration = (swipeConfig?.duration ?: 500).toLong()
                service.swipe(startX, startY, endX, endY, duration)
            }
            "swipe_down" -> {
                val startX = (swipeConfig?.startXRatio ?: 0.5f) * screenWidth
                val startY = (swipeConfig?.startYRatio ?: 0.3f) * screenHeight
                val endX = (swipeConfig?.endXRatio ?: 0.5f) * screenWidth
                val endY = (swipeConfig?.endYRatio ?: 0.7f) * screenHeight
                val duration = (swipeConfig?.duration ?: 500).toLong()
                service.swipe(startX, startY, endX, endY, duration)
            }
            "swipe_left" -> {
                val startX = (swipeConfig?.startXRatio ?: 0.8f) * screenWidth
                val startY = (swipeConfig?.startYRatio ?: 0.5f) * screenHeight
                val endX = (swipeConfig?.endXRatio ?: 0.2f) * screenWidth
                val endY = (swipeConfig?.endYRatio ?: 0.5f) * screenHeight
                val duration = (swipeConfig?.duration ?: 500).toLong()
                service.swipe(startX, startY, endX, endY, duration)
            }
            "swipe_right" -> {
                val startX = (swipeConfig?.startXRatio ?: 0.2f) * screenWidth
                val startY = (swipeConfig?.startYRatio ?: 0.5f) * screenHeight
                val endX = (swipeConfig?.endXRatio ?: 0.8f) * screenWidth
                val endY = (swipeConfig?.endYRatio ?: 0.5f) * screenHeight
                val duration = (swipeConfig?.duration ?: 500).toLong()
                service.swipe(startX, startY, endX, endY, duration)
            }
            else -> false
        }
    }
    
    private fun findNodeByText(
        service: AutoAccessibilityService, 
        text: String, 
        mode: String
    ): AccessibilityNodeInfo? {
        val root = service.getRootNode() ?: return null
        val nodes = root.findAccessibilityNodeInfosByText(text)
        
        return when (mode) {
            "exact" -> nodes.find { it.text?.toString() == text }
            "contains" -> nodes.find { it.text?.toString()?.contains(text, ignoreCase = true) == true }
            "regex" -> nodes.find { it.text?.toString()?.matches(Regex(text)) == true }
            else -> nodes.firstOrNull()
        }
    }
    
    suspend fun performOperation(
        node: AccessibilityNodeInfo,
        operation: String,
        params: Map<String, Any>? = null
    ): Boolean {
        val service = AutoAccessibilityService.getInstance() ?: return false
        
        val delayBefore = (params?.get("delay_before") as? Long) ?: RandomUtils.randomDelay()
        delay(delayBefore)
        
        val result = when (operation) {
            "click" -> performClick(service, node)
            "long_click" -> performLongClick(service, node, params)
            "double_click" -> performDoubleClick(service, node, params)
            "swipe" -> performSwipe(service, node, params)
            "input" -> performInput(service, node, params)
            "scroll_up" -> service.scroll(AutoAccessibilityService.ScrollDirection.UP)
            "scroll_down" -> service.scroll(AutoAccessibilityService.ScrollDirection.DOWN)
            "scroll_left" -> service.scroll(AutoAccessibilityService.ScrollDirection.LEFT)
            "scroll_right" -> service.scroll(AutoAccessibilityService.ScrollDirection.RIGHT)
            else -> false
        }
        
        val delayAfter = (params?.get("delay_after") as? Long) ?: RandomUtils.randomDelay(300, 800)
        delay(delayAfter)
        
        return result
    }
    
    private fun performClick(service: AutoAccessibilityService, node: AccessibilityNodeInfo): Boolean {
        return service.clickNode(node)
    }
    
    private fun performLongClick(
        service: AutoAccessibilityService, 
        node: AccessibilityNodeInfo, 
        params: Map<String, Any>?
    ): Boolean {
        val duration = (params?.get("duration") as? Long) ?: 500L
        return service.longClickNode(node, duration)
    }
    
    private suspend fun performDoubleClick(
        service: AutoAccessibilityService, 
        node: AccessibilityNodeInfo,
        params: Map<String, Any>? = null
    ): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val interval = (params?.get("interval") as? Long) ?: 100L
        
        val firstClick = service.clickAtPosition(rect.centerX(), rect.centerY())
        if (!firstClick) return false
        
        delay(interval + RandomUtils.randomBetween(0, 50))
        
        return service.clickAtPosition(rect.centerX(), rect.centerY())
    }
    
    private fun performSwipe(
        service: AutoAccessibilityService, 
        node: AccessibilityNodeInfo, 
        params: Map<String, Any>?
    ): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        
        val direction = params?.get("direction") as? String ?: "up"
        val distance = (params?.get("distance") as? Float) ?: 0.5f
        
        val centerX = rect.centerX().toFloat()
        val centerY = rect.centerY().toFloat()
        val height = rect.height().toFloat()
        val width = rect.width().toFloat()
        
        return when (direction) {
            "up" -> service.swipe(centerX, centerY + height * distance, centerX, centerY - height * distance)
            "down" -> service.swipe(centerX, centerY - height * distance, centerX, centerY + height * distance)
            "left" -> service.swipe(centerX + width * distance, centerY, centerX - width * distance, centerY)
            "right" -> service.swipe(centerX - width * distance, centerY, centerX + width * distance, centerY)
            else -> false
        }
    }
    
    private suspend fun performInput(
        service: AutoAccessibilityService, 
        node: AccessibilityNodeInfo, 
        params: Map<String, Any>?
    ): Boolean {
        val text = params?.get("text") as? String ?: return false
        val clearBefore = params?.get("clear_before") as? Boolean ?: false
        
        if (clearBefore) {
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            })
            delay(100)
        }
        
        return service.inputText(node, text)
    }
    
    private suspend fun executePopupSkip(service: AutoAccessibilityService, config: ControlConfig): Boolean {
        val popupConfigs = config.popupConfigs ?: return false
        
        val sortedPopups = popupConfigs.sortedBy { it.priority }
        
        for (popupConfig in sortedPopups) {
            val popupNode = when (popupConfig.matchType) {
                "text" -> findNodeByText(service, popupConfig.matchValue, "contains")
                "description" -> service.findNodeByDescription(popupConfig.matchValue)
                "id" -> service.findNodeById(popupConfig.matchValue)
                else -> null
            }
            
            if (popupNode != null) {
                val closeNode = when (popupConfig.closeMatchType) {
                    "text" -> findNodeByText(service, popupConfig.closeMatchValue, "contains")
                    "description" -> service.findNodeByDescription(popupConfig.closeMatchValue)
                    "id" -> service.findNodeById(popupConfig.closeMatchValue)
                    else -> null
                }
                
                if (closeNode != null) {
                    Log.d(TAG, "Closing popup: ${popupConfig.popupId}")
                    val result = service.clickNode(closeNode)
                    delay(500)
                    return result
                }
            }
        }
        
        return false
    }
    
    private suspend fun executeBatchOperation(service: AutoAccessibilityService, config: ControlConfig): Boolean {
        val batchConfig = config.batchConfig ?: return false
        val batchCount = batchConfig.batchCount
        
        Log.d(TAG, "Starting batch operation, count: $batchCount")
        
        for (i in 0 until batchCount) {
            Log.d(TAG, "Batch operation ${i + 1}/$batchCount")
            
            val node = findControl(config)
            if (node != null) {
                performOperation(node, config.operation, mapOf(
                    "duration" to config.longPressDuration,
                    "interval" to config.doubleClickInterval
                ))
            } else {
                Log.w(TAG, "Node not found in batch operation ${i + 1}")
            }
            
            if (batchConfig.scrollAfterEach && i < batchCount - 1) {
                val scrollConfig = batchConfig.scrollConfig
                if (scrollConfig != null) {
                    val root = service.getRootNode() ?: continue
                    val rootBounds = Rect()
                    root.getBoundsInScreen(rootBounds)
                    val screenWidth = rootBounds.width().toFloat()
                    val screenHeight = rootBounds.height().toFloat()
                    
                    val startX = scrollConfig.startXRatio * screenWidth
                    val startY = scrollConfig.startYRatio * screenHeight
                    val endX = scrollConfig.endXRatio * screenWidth
                    val endY = scrollConfig.endYRatio * screenHeight
                    
                    service.swipe(startX, startY, endX, endY, scrollConfig.duration.toLong())
                } else {
                    service.scroll(AutoAccessibilityService.ScrollDirection.UP)
                }
                delay(500)
            }
        }
        
        return true
    }
    
    suspend fun findControlWithScroll(config: ControlConfig): AccessibilityNodeInfo? {
        val service = AutoAccessibilityService.getInstance() ?: return null
        val scrollConfig = config.scrollOnNotFound
        
        if (scrollConfig == null) {
            return findControl(config)
        }
        
        var node = findControl(config)
        if (node != null) return node
        
        val maxScrolls = scrollConfig.maxScrolls
        val direction = when (scrollConfig.direction) {
            "up" -> AutoAccessibilityService.ScrollDirection.UP
            "down" -> AutoAccessibilityService.ScrollDirection.DOWN
            else -> AutoAccessibilityService.ScrollDirection.UP
        }
        
        for (i in 0 until maxScrolls) {
            Log.d(TAG, "Scrolling to find element, attempt ${i + 1}/$maxScrolls")
            
            service.scroll(direction)
            delay(500)
            
            node = findControl(config)
            if (node != null) {
                Log.d(TAG, "Found element after ${i + 1} scrolls")
                return node
            }
        }
        
        Log.w(TAG, "Element not found after $maxScrolls scrolls")
        return null
    }
    
    fun getNodeBounds(node: AccessibilityNodeInfo): Rect {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect
    }
    
    fun getNodeText(node: AccessibilityNodeInfo): String? {
        return node.text?.toString() ?: node.contentDescription?.toString()
    }
    
    fun isNodeVisible(node: AccessibilityNodeInfo): Boolean {
        return node.isVisibleToUser
    }
    
    fun isNodeClickable(node: AccessibilityNodeInfo): Boolean {
        return node.isClickable || node.parent?.isClickable == true
    }
}
