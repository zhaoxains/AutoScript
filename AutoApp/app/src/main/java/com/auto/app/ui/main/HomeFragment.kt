package com.auto.app.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.auto.app.databinding.FragmentHomeBinding
import com.auto.app.service.AutoAccessibilityService
import com.auto.app.service.FloatingDebugManager
import com.auto.app.service.ScreenCaptureService
import com.auto.app.ui.log.LogDetailActivity
import com.auto.app.util.NodeDebugger
import com.auto.app.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var scriptAdapter: ScriptAdapter
    private lateinit var recordAdapter: RecordAdapter
    
    private val OVERLAY_PERMISSION_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
        checkPermissions()
    }
    
    private fun setupRecyclerViews() {
        scriptAdapter = ScriptAdapter(
            onItemClick = { script ->
                viewModel.selectScript(script)
            },
            onExecuteClick = { script ->
                if (checkAccessibilityService()) {
                    viewModel.executeScript(script)
                    Toast.makeText(requireContext(), "开始执行: ${script.scriptName}", Toast.LENGTH_SHORT).show()
                }
            }
        )
        
        binding.rvScripts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scriptAdapter
        }
        
        recordAdapter = RecordAdapter(
            onItemClick = { record ->
                val intent = Intent(requireContext(), LogDetailActivity::class.java)
                intent.putExtra("record_id", record.id)
                startActivity(intent)
            }
        )
        
        binding.rvRecords.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recordAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.btnStartMonitor.setOnClickListener {
            if (checkAccessibilityService()) {
                viewModel.startMonitoring()
                val message = if (viewModel.isMonitoring.value) "停止监控" else "开始监控"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnStopMonitor.setOnClickListener {
            viewModel.stopExecution()
            Toast.makeText(requireContext(), "已停止执行", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnExecuteAll.setOnClickListener {
            android.util.Log.d("HomeFragment", "btnExecuteAll clicked")
            if (checkAccessibilityService()) {
                android.util.Log.d("HomeFragment", "checkAccessibilityService returned true")
                viewModel.executeAllScripts()
                Toast.makeText(requireContext(), "开始执行所有上线脚本", Toast.LENGTH_SHORT).show()
            } else {
                android.util.Log.d("HomeFragment", "checkAccessibilityService returned false")
            }
        }
        
        binding.btnDebugNodes.setOnClickListener {
            showDebugDialog()
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }
    }
    
    private fun showDebugDialog() {
        if (!AutoAccessibilityService.isServiceEnabled()) {
            Toast.makeText(requireContext(), "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
            return
        }
        
        val options = arrayOf("🔍 开启悬浮窗调试", "查看所有可点击节点", "查看带描述的节点", "导出JSON到日志", "查看完整节点树")
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("节点调试")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startFloatingDebug()
                    1 -> showClickableNodes()
                    2 -> showNodesWithDescription()
                    3 -> exportToJson()
                    4 -> showAllNodes()
                }
            }
            .show()
    }
    
    private fun startFloatingDebug() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(requireContext())) {
            Toast.makeText(requireContext(), "请授予悬浮窗权限", Toast.LENGTH_SHORT).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireContext().packageName}")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        } else {
            showFloatingDebug()
        }
    }
    
    private fun showFloatingDebug() {
        val debugManager = FloatingDebugManager.getInstance(requireContext())
        if (debugManager.isShowing()) {
            debugManager.hide()
            Toast.makeText(requireContext(), "悬浮窗调试已关闭", Toast.LENGTH_SHORT).show()
        } else {
            debugManager.showFloatingButton()
            Toast.makeText(requireContext(), "悬浮窗调试已开启\n\n请打开目标APP，然后点击悬浮按钮开始调试", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(requireContext())) {
                showFloatingDebug()
            } else {
                Toast.makeText(requireContext(), "悬浮窗权限未授予", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showClickableNodes() {
        val result = NodeDebugger.dumpClickableNodes()
        showResultDialog("可点击节点", result)
    }
    
    private fun showNodesWithDescription() {
        val result = NodeDebugger.dumpNodesWithDescription()
        showResultDialog("带描述的节点", result)
    }
    
    private fun showAllNodes() {
        val result = NodeDebugger.dumpAllNodes()
        showResultDialog("完整节点树", result)
    }
    
    private fun exportToJson() {
        val json = NodeDebugger.exportToJson()
        android.util.Log.d("NodeDebug", json)
        Toast.makeText(requireContext(), "已导出到Logcat，标签: NodeDebug", Toast.LENGTH_LONG).show()
    }
    
    private fun showResultDialog(title: String, content: String) {
        val scrollView = android.widget.ScrollView(requireContext())
        val textView = android.widget.TextView(requireContext())
        textView.text = content
        textView.textSize = 12f
        textView.setPadding(16, 16, 16, 16)
        scrollView.addView(textView)
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(scrollView)
            .setPositiveButton("关闭", null)
            .setNegativeButton("复制") { _, _ ->
                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText(title, content)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scripts.collect { scripts ->
                scriptAdapter.submitList(scripts)
                binding.swipeRefresh.isRefreshing = false
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.records.collect { records ->
                recordAdapter.submitList(records.take(10))
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isMonitoring.collect { isMonitoring ->
                updateMonitoringState(isMonitoring)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentStatus.collect { status ->
                binding.tvStatus.text = status
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentScript.collect { script ->
                script?.let {
                    binding.tvCurrentTask.text = "当前任务: ${it.scriptName}"
                } ?: run {
                    binding.tvCurrentTask.text = "当前任务: 无"
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.executionQueue.collect { queue ->
                if (queue.isNotEmpty()) {
                    binding.tvQueueInfo.text = "队列: ${queue.size} 个脚本待执行"
                    binding.tvQueueInfo.visibility = View.VISIBLE
                } else {
                    binding.tvQueueInfo.visibility = View.GONE
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }
    
    private fun updateMonitoringState(isMonitoring: Boolean) {
        if (isMonitoring) {
            binding.btnStartMonitor.text = "停止监控"
            binding.btnStartMonitor.setBackgroundColor(resources.getColor(com.auto.app.R.color.error, null))
            binding.btnStopMonitor.visibility = View.VISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.btnStartMonitor.text = "开始监控"
            binding.btnStartMonitor.setBackgroundColor(resources.getColor(com.auto.app.R.color.primary, null))
            binding.btnStopMonitor.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
        }
    }
    
    private fun checkPermissions() {
        val accessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(
            requireContext(), AutoAccessibilityService::class.java
        )
        
        if (!accessibilityEnabled) {
            binding.tvAccessibilityStatus.visibility = View.VISIBLE
            binding.tvAccessibilityStatus.text = "无障碍服务未开启"
            binding.tvAccessibilityStatus.setTextColor(resources.getColor(com.auto.app.R.color.error, null))
            binding.tvAccessibilityStatus.setOnClickListener {
                PermissionUtils.openAccessibilitySettings(requireContext())
            }
        } else {
            binding.tvAccessibilityStatus.visibility = View.GONE
        }
        
        val screenshotEnabled = ScreenCaptureService.hasPermission()
        if (!screenshotEnabled) {
            binding.tvScreenshotStatus.visibility = View.VISIBLE
            binding.tvScreenshotStatus.text = "截图权限未开启（点击前往授权）"
            binding.tvScreenshotStatus.setTextColor(resources.getColor(com.auto.app.R.color.error, null))
            binding.tvScreenshotStatus.setOnClickListener {
                (requireActivity() as MainActivity).requestScreenCapturePermission()
            }
        } else {
            binding.tvScreenshotStatus.visibility = View.GONE
        }
    }
    
    private fun checkAccessibilityService(): Boolean {
        android.util.Log.d("HomeFragment", "checkAccessibilityService called")
        val enabled = AutoAccessibilityService.isServiceEnabled()
        android.util.Log.d("HomeFragment", "AutoAccessibilityService.isServiceEnabled() = $enabled")
        if (!enabled) {
            Toast.makeText(requireContext(), "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
            PermissionUtils.openAccessibilitySettings(requireContext())
        }
        return enabled
    }
    
    override fun onResume() {
        super.onResume()
        checkPermissions()
        viewModel.refreshData()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        FloatingDebugManager.getInstance(requireContext()).hide()
        _binding = null
    }
}
