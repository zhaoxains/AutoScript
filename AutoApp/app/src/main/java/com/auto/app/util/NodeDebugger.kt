package com.auto.app.util

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.auto.app.service.AutoAccessibilityService
import org.json.JSONArray
import org.json.JSONObject

object NodeDebugger {
    
    private const val TAG = "NodeDebugger"
    
    fun dumpAllNodes(): String {
        val service = AutoAccessibilityService.getInstance() ?: return "Service not available"
        val root = service.getRootNode() ?: return "Root node not available"
        
        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        val screenWidth = rootBounds.width()
        val screenHeight = rootBounds.height()
        
        val sb = StringBuilder()
        sb.appendLine("=== Screen Size: ${screenWidth}x${screenHeight} ===")
        sb.appendLine()
        
        dumpNode(root, 0, sb, screenWidth, screenHeight)
        
        return sb.toString()
    }
    
    fun dumpClickableNodes(): String {
        val service = AutoAccessibilityService.getInstance() ?: return "Service not available"
        val root = service.getRootNode() ?: return "Root node not available"
        
        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        val screenWidth = rootBounds.width()
        val screenHeight = rootBounds.height()
        
        val sb = StringBuilder()
        sb.appendLine("=== Clickable Nodes ===")
        sb.appendLine("Screen Size: ${screenWidth}x${screenHeight}")
        sb.appendLine()
        
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        collectClickableNodes(root, clickableNodes)
        
        clickableNodes.forEachIndexed { index, node ->
            sb.appendLine("--- Node ${index + 1} ---")
            dumpNodeInfo(node, sb, screenWidth, screenHeight)
            sb.appendLine()
        }
        
        return sb.toString()
    }
    
    fun dumpNodesWithDescription(): String {
        val service = AutoAccessibilityService.getInstance() ?: return "Service not available"
        val root = service.getRootNode() ?: return "Root node not available"
        
        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        val screenWidth = rootBounds.width()
        val screenHeight = rootBounds.height()
        
        val sb = StringBuilder()
        sb.appendLine("=== Nodes with ContentDescription ===")
        sb.appendLine("Screen Size: ${screenWidth}x${screenHeight}")
        sb.appendLine()
        
        val nodesWithDesc = mutableListOf<AccessibilityNodeInfo>()
        collectNodesWithDescription(root, nodesWithDesc)
        
        nodesWithDesc.forEachIndexed { index, node ->
            sb.appendLine("--- Node ${index + 1} ---")
            dumpNodeInfo(node, sb, screenWidth, screenHeight)
            sb.appendLine()
        }
        
        return sb.toString()
    }
    
    fun exportToJson(): String {
        val service = AutoAccessibilityService.getInstance() ?: return "{}"
        val root = service.getRootNode() ?: return "{}"
        
        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        val screenWidth = rootBounds.width()
        val screenHeight = rootBounds.height()
        
        val jsonArray = JSONArray()
        collectNodesJson(root, jsonArray, screenWidth, screenHeight)
        
        return jsonArray.toString(2)
    }
    
    private fun dumpNode(
        node: AccessibilityNodeInfo,
        depth: Int,
        sb: StringBuilder,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val indent = "  ".repeat(depth)
        
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        val xRatio = bounds.centerX().toFloat() / screenWidth
        val yRatio = bounds.centerY().toFloat() / screenHeight
        
        sb.appendLine("${indent}[${node.className}]")
        sb.appendLine("${indent}  text: ${node.text}")
        sb.appendLine("${indent}  contentDescription: ${node.contentDescription}")
        sb.appendLine("${indent}  viewIdResourceName: ${node.viewIdResourceName}")
        sb.appendLine("${indent}  clickable: ${node.isClickable}")
        sb.appendLine("${indent}  enabled: ${node.isEnabled}")
        sb.appendLine("${indent}  visible: ${node.isVisibleToUser}")
        sb.appendLine("${indent}  bounds: ${bounds}")
        sb.appendLine("${indent}  position: (${String.format("%.3f", xRatio)}, ${String.format("%.3f", yRatio)})")
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                dumpNode(child, depth + 1, sb, screenWidth, screenHeight)
            }
        }
    }
    
    private fun dumpNodeInfo(
        node: AccessibilityNodeInfo,
        sb: StringBuilder,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        val xRatio = bounds.centerX().toFloat() / screenWidth
        val yRatio = bounds.centerY().toFloat() / screenHeight
        
        sb.appendLine("className: ${node.className}")
        sb.appendLine("text: ${node.text}")
        sb.appendLine("contentDescription: ${node.contentDescription}")
        sb.appendLine("viewIdResourceName: ${node.viewIdResourceName}")
        sb.appendLine("clickable: ${node.isClickable}")
        sb.appendLine("enabled: ${node.isEnabled}")
        sb.appendLine("visible: ${node.isVisibleToUser}")
        sb.appendLine("bounds: $bounds")
        sb.appendLine("position: (x_ratio: ${String.format("%.3f", xRatio)}, y_ratio: ${String.format("%.3f", yRatio)})")
        
        node.parent?.let { parent ->
            sb.appendLine("parent.text: ${parent.text}")
            sb.appendLine("parent.contentDescription: ${parent.contentDescription}")
        }
        
        node.parent?.let { parent ->
            val siblings = mutableListOf<String>()
            for (i in 0 until parent.childCount) {
                val sibling = parent.getChild(i)
                if (sibling != null && sibling != node) {
                    sibling.text?.toString()?.let { siblings.add(it) }
                    sibling.contentDescription?.toString()?.let { siblings.add(it) }
                }
            }
            if (siblings.isNotEmpty()) {
                sb.appendLine("siblingTexts: ${siblings.take(5).joinToString(", ")}")
            }
        }
    }
    
    private fun collectClickableNodes(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (node.isClickable || node.parent?.isClickable == true) {
            result.add(node)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectClickableNodes(child, result)
            }
        }
    }
    
    private fun collectNodesWithDescription(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (!node.contentDescription.isNullOrEmpty()) {
            result.add(node)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectNodesWithDescription(child, result)
            }
        }
    }
    
    private fun collectNodesJson(
        node: AccessibilityNodeInfo,
        jsonArray: JSONArray,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        val json = JSONObject()
        json.put("className", node.className?.toString() ?: "")
        json.put("text", node.text?.toString() ?: "")
        json.put("contentDescription", node.contentDescription?.toString() ?: "")
        json.put("viewIdResourceName", node.viewIdResourceName ?: "")
        json.put("clickable", node.isClickable)
        json.put("enabled", node.isEnabled)
        json.put("visible", node.isVisibleToUser)
        json.put("boundsLeft", bounds.left)
        json.put("boundsTop", bounds.top)
        json.put("boundsRight", bounds.right)
        json.put("boundsBottom", bounds.bottom)
        json.put("xRatio", bounds.centerX().toFloat() / screenWidth)
        json.put("yRatio", bounds.centerY().toFloat() / screenHeight)
        
        jsonArray.put(json)
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectNodesJson(child, jsonArray, screenWidth, screenHeight)
            }
        }
    }
}
