package com.auto.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.auto.app.R

data class NodeInfo(
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val viewId: String?,
    val clickable: Boolean,
    val bounds: Rect,
    val xRatio: Float,
    val yRatio: Float
)

class FloatingDebugManager(private val context: Context) {
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var captureButton: View? = null
    private var infoWindow: View? = null
    private var overlayView: View? = null
    private var isCaptureMode = false
    
    companion object {
        @Volatile
        private var instance: FloatingDebugManager? = null
        
        fun getInstance(context: Context): FloatingDebugManager {
            return instance ?: synchronized(this) {
                instance ?: FloatingDebugManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    fun showFloatingButton() {
        if (captureButton != null) return
        
        mainHandler.post {
            try {
                createCaptureButton()
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("创建悬浮按钮失败: ${e.message}")
            }
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun createCaptureButton() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }
        
        captureButton = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20, 8, 20, 8)
            setBackgroundColor(0xFF2196F3.toInt())
            
            addView(Button(context).apply {
                text = "捕获"
                setBackgroundColor(0x00000000)
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
                isClickable = false
                isFocusable = false
            })
            
            addView(TextView(context).apply {
                text = "点击开始"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 9f
                gravity = Gravity.CENTER
            })
            
            setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isMoved = false
                
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = layoutParams.x
                            initialY = layoutParams.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isMoved = false
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()
                            if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                                isMoved = true
                            }
                            layoutParams.x = initialX + dx
                            layoutParams.y = initialY + dy
                            windowManager.updateViewLayout(captureButton, layoutParams)
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!isMoved) {
                                toggleCaptureMode()
                            }
                            return true
                        }
                    }
                    return false
                }
            })
            
            setOnLongClickListener {
                hide()
                showToast("调试工具已关闭")
                true
            }
        }
        
        windowManager.addView(captureButton, layoutParams)
    }
    
    private fun updateStatus(buttonText: String, statusText: String, isActive: Boolean = false) {
        mainHandler.post {
            (captureButton as? android.widget.LinearLayout)?.apply {
                if (isActive) {
                    setBackgroundColor(0xFFFF5722.toInt())
                } else {
                    setBackgroundColor(0xFF2196F3.toInt())
                }
                
                getChildAt(0)?.let { btn ->
                    (btn as? Button)?.setText(buttonText)
                }
                
                getChildAt(1)?.let { tv ->
                    (tv as? TextView)?.text = statusText
                }
            }
        }
    }
    
    private fun toggleCaptureMode() {
        isCaptureMode = !isCaptureMode
        
        if (isCaptureMode) {
            updateStatus("捕获中", "请点击目标", true)
            mainHandler.post { showOverlay() }
        } else {
            updateStatus("捕获", "点击开始", false)
            mainHandler.post { hideOverlay() }
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay() {
        if (overlayView != null) return
        
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        
        overlayView = View(context).apply {
            setBackgroundColor(0x00000000)
            
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE || event.action == MotionEvent.ACTION_DOWN) {
                    val x = event.rawX.toInt()
                    val y = event.rawY.toInt()
                    
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        captureNodeAtPosition(x, y)
                    }
                    true
                } else {
                    false
                }
            }
        }
        
        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("显示覆盖层失败: ${e.message}")
        }
    }
    
    private fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
        isCaptureMode = false
    }
    
    private fun captureNodeAtPosition(x: Int, y: Int) {
        android.util.Log.d("FloatingDebug", "Capturing node at: ($x, $y)")
        
        hideOverlay()
        updateStatus("捕获中", "正在解析...", true)
        
        try {
            val service = AutoAccessibilityService.getInstance()
            if (service == null) {
                updateStatus("捕获", "无障碍未开启", false)
                return
            }
            
            val root = service.getRootNode()
            if (root == null) {
                updateStatus("捕获", "获取节点失败", false)
                return
            }
            
            val rootBounds = Rect()
            root.getBoundsInScreen(rootBounds)
            val screenWidth = rootBounds.width()
            val screenHeight = rootBounds.height()
            
            val nodes = mutableListOf<NodeInfo>()
            collectNodesAtPosition(root, x, y, screenWidth, screenHeight, nodes)
            
            root.recycle()
            
            android.util.Log.d("FloatingDebug", "Found ${nodes.size} nodes at ($x, $y)")
            
            if (nodes.isEmpty()) {
                val searchRadius = 50
                for (dx in -searchRadius..searchRadius step 10) {
                    for (dy in -searchRadius..searchRadius step 10) {
                        val nx = x + dx
                        val ny = y + dy
                        val root2 = service.getRootNode()
                        if (root2 != null) {
                            collectNodesAtPosition(root2, nx, ny, screenWidth, screenHeight, nodes)
                            root2.recycle()
                        }
                    }
                }
                
                val uniqueNodes = nodes.distinctBy { it.bounds.toString() + it.contentDescription }
                nodes.clear()
                nodes.addAll(uniqueNodes)
            }
            
            if (nodes.isEmpty()) {
                updateStatus("捕获", "未找到节点", false)
                return
            }
            
            nodes.sortBy { 
                val bounds = it.bounds
                val dx = bounds.centerX() - x
                val dy = bounds.centerY() - y
                dx * dx + dy * dy
            }
            
            updateStatus("捕获", "点击开始", false)
            
            if (nodes.size == 1) {
                showInfoWindow(x, y, nodes[0])
            } else {
                showNodeSelector(x, y, nodes)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            updateStatus("捕获", "错误: ${e.message}", false)
        }
    }
    
    private fun collectNodesAtPosition(
        node: AccessibilityNodeInfo, 
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int,
        result: MutableList<NodeInfo>
    ) {
        try {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            
            if (bounds.contains(x, y)) {
                val xRatio = bounds.centerX().toFloat() / screenWidth
                val yRatio = bounds.centerY().toFloat() / screenHeight
                
                val hasContent = !node.contentDescription.isNullOrEmpty() || 
                                 !node.text.isNullOrEmpty() || 
                                 node.isClickable
                
                if (hasContent) {
                    result.add(NodeInfo(
                        className = node.className?.toString(),
                        text = node.text?.toString(),
                        contentDescription = node.contentDescription?.toString(),
                        viewId = node.viewIdResourceName,
                        clickable = node.isClickable,
                        bounds = Rect(bounds),
                        xRatio = xRatio,
                        yRatio = yRatio
                    ))
                }
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    collectNodesAtPosition(child, x, y, screenWidth, screenHeight, result)
                    child.recycle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun showNodeSelector(x: Int, y: Int, nodes: List<NodeInfo>) {
        val options = nodes.take(10).mapIndexed { index, node ->
            val desc = node.contentDescription?.take(20) ?: ""
            val text = node.text?.take(20) ?: ""
            val className = node.className?.substringAfterLast(".") ?: ""
            val clickMark = if (node.clickable) "✓" else "○"
            "$clickMark ${index + 1}. $className ${if (desc.isNotEmpty()) "[$desc]" else ""} ${if (text.isNotEmpty()) "\"$text\"" else ""}"
        }.toTypedArray()
        
        val builder = android.app.AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert)
        builder.setTitle("选择节点 (${nodes.size}个)")
            .setItems(options) { _, which ->
                showInfoWindow(x, y, nodes[which])
            }
            .setNegativeButton("取消", null)
        
        val dialog = builder.create()
        dialog.window?.setType(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        })
        dialog.show()
    }
    
    @SuppressLint("InflateParams")
    private fun showInfoWindow(x: Int, y: Int, node: NodeInfo) {
        hideInfoWindow()
        
        val info = buildString {
            appendLine("className: ${node.className}")
            appendLine("text: ${node.text}")
            appendLine("contentDescription: ${node.contentDescription}")
            appendLine("viewId: ${node.viewId}")
            appendLine("clickable: ${node.clickable}")
            appendLine("bounds: ${node.bounds}")
            appendLine("position: (${String.format("%.3f", node.xRatio)}, ${String.format("%.3f", node.yRatio)})")
        }
        
        val displayMetrics = context.resources.displayMetrics
        val screenW = displayMetrics.widthPixels
        
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = if (x + 300 > screenW) x - 300 else x + 20
            this.y = y - 50
        }
        
        val inflater = LayoutInflater.from(context)
        infoWindow = inflater.inflate(R.layout.floating_debug_info, null).apply {
            findViewById<TextView>(R.id.tvNodeInfo).text = info
            
            findViewById<View>(R.id.btnClose).setOnClickListener {
                hideInfoWindow()
            }
            
            findViewById<View>(R.id.btnCopy).setOnClickListener {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("node_info", info)
                clipboard.setPrimaryClip(clip)
                showToast("已复制")
            }
            
            findViewById<View>(R.id.btnClick).setOnClickListener {
                clickNode(node)
                hideInfoWindow()
            }
        }
        
        try {
            windowManager.addView(infoWindow, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("显示信息窗口失败: ${e.message}")
        }
    }
    
    private fun clickNode(node: NodeInfo) {
        try {
            val service = AutoAccessibilityService.getInstance()
            if (service == null) {
                showToast("无障碍服务未开启")
                return
            }
            
            var clickX = node.bounds.centerX()
            var clickY = node.bounds.centerY()
            
            android.util.Log.d("FloatingDebug", "Original node bounds: ${node.bounds}, clickable: ${node.clickable}")
            
            if (!node.clickable) {
                android.util.Log.d("FloatingDebug", "Node not clickable, searching for clickable parent...")
                
                val root = service.getRootNode()
                if (root != null) {
                    val clickableParent = findClickableParent(root, node.bounds)
                    if (clickableParent != null) {
                        val parentBounds = Rect()
                        clickableParent.getBoundsInScreen(parentBounds)
                        clickX = parentBounds.centerX()
                        clickY = parentBounds.centerY()
                        android.util.Log.d("FloatingDebug", "Found clickable parent: $parentBounds")
                        showToast("使用父节点坐标")
                    }
                    root.recycle()
                }
            }
            
            android.util.Log.d("FloatingDebug", "Clicking at: ($clickX, $clickY)")
            
            val result = service.clickAtPosition(clickX, clickY)
            
            android.util.Log.d("FloatingDebug", "Click result: $result")
            
            val desc = node.contentDescription ?: node.text ?: "坐标($clickX,$clickY)"
            showToast(if (result) "点击: $desc" else "点击失败")
            
        } catch (e: Exception) {
            android.util.Log.e("FloatingDebug", "Click error", e)
            showToast("点击失败: ${e.message}")
        }
    }
    
    private fun findClickableParent(node: AccessibilityNodeInfo, targetBounds: Rect): AccessibilityNodeInfo? {
        try {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            
            if (bounds.contains(targetBounds) && node.isClickable) {
                return node
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    val result = findClickableParent(child, targetBounds)
                    if (result != null) {
                        return result
                    }
                    child.recycle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun hideInfoWindow() {
        infoWindow?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            infoWindow = null
        }
    }
    
    fun hide() {
        mainHandler.post {
            hideOverlay()
            hideInfoWindow()
            captureButton?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                captureButton = null
            }
        }
    }
    
    fun isShowing(): Boolean = captureButton != null
}
