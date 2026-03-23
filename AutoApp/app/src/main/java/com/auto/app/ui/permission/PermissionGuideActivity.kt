package com.auto.app.ui.permission

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.auto.app.databinding.ActivityPermissionGuideBinding
import com.auto.app.service.AutoAccessibilityService
import com.auto.app.ui.main.MainActivity
import com.auto.app.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PermissionGuideActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPermissionGuideBinding
    private var checkJob: Job? = null
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        checkPermissions()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        checkPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        checkPermissions()
        startPermissionCheck()
    }
    
    override fun onPause() {
        super.onPause()
        stopPermissionCheck()
    }
    
    private fun setupViews() {
        binding.btnAccessibility.setOnClickListener {
            PermissionUtils.openAccessibilitySettings(this)
        }
        
        binding.btnOverlay.setOnClickListener {
            if (!PermissionUtils.canDrawOverlays(this)) {
                overlayPermissionLauncher.launch(
                    android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    )
                )
            }
        }
        
        binding.btnBattery.setOnClickListener {
            PermissionUtils.openBatteryOptimizationSettings(this)
        }
        
        binding.btnAutoStart.setOnClickListener {
            PermissionUtils.openAutoStartSettings(this)
        }
        
        binding.btnContinue.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun checkPermissions() {
        val accessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(
            this, AutoAccessibilityService::class.java
        )
        val overlayEnabled = PermissionUtils.canDrawOverlays(this)
        val batteryOptimizationIgnored = PermissionUtils.isIgnoringBatteryOptimizations(this)
        
        updatePermissionStatus(
            binding.itemAccessibility,
            accessibilityEnabled,
            "无障碍服务",
            "用于识别和操作屏幕控件"
        )
        
        updatePermissionStatus(
            binding.itemOverlay,
            overlayEnabled,
            "悬浮窗权限",
            "用于显示执行状态浮窗"
        )
        
        updatePermissionStatus(
            binding.itemBattery,
            batteryOptimizationIgnored,
            "电池优化",
            "忽略电池优化，防止后台被杀"
        )
        
        val allGranted = accessibilityEnabled && overlayEnabled && batteryOptimizationIgnored
        
        binding.btnContinue.isEnabled = accessibilityEnabled
        binding.btnContinue.alpha = if (accessibilityEnabled) 1f else 0.5f
        
        if (allGranted) {
            binding.tvStatus.text = "所有权限已开启，可以开始使用"
            binding.tvStatus.setTextColor(getColor(com.auto.app.R.color.success))
        } else {
            binding.tvStatus.text = "请开启必要权限以使用自动化功能"
            binding.tvStatus.setTextColor(getColor(com.auto.app.R.color.warning))
        }
    }
    
    private fun updatePermissionStatus(
        view: PermissionItemView,
        isGranted: Boolean,
        title: String,
        description: String
    ) {
        view.setTitle(title)
        view.setDescription(description)
        view.setGranted(isGranted)
    }
    
    private fun startPermissionCheck() {
        checkJob = lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                withContext(Dispatchers.Main) {
                    checkPermissions()
                }
            }
        }
    }
    
    private fun stopPermissionCheck() {
        checkJob?.cancel()
        checkJob = null
    }
}
