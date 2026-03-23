package com.auto.app.ui.profile

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.auto.app.R
import com.auto.app.databinding.FragmentProfileBinding
import com.auto.app.service.ScreenCaptureService
import com.auto.app.ui.login.LoginActivity
import com.auto.app.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    private val mediaProjectionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            ScreenCaptureService.setMediaProjectionResult(result.resultCode, result.data!!)
            Toast.makeText(requireContext(), "截图权限已授权", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "截图权限被拒绝", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeState()
    }

    private fun setupViews() {
        binding.itemAccessibility.setOnClickListener {
            PermissionUtils.openAccessibilitySettings(requireContext())
        }

        binding.itemBattery.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }

        binding.itemClearCache.setOnClickListener {
            viewModel.clearCache()
            Toast.makeText(requireContext(), "缓存已清除", Toast.LENGTH_SHORT).show()
        }

        binding.itemScreenCapture.setOnClickListener {
            requestScreenCapturePermission()
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun requestScreenCapturePermission() {
        val mediaProjectionManager = requireContext().getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(intent)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    state.user?.let { user ->
                        binding.tvUsername.text = user.username
                        binding.tvRole.text = when (user.role) {
                            9 -> "管理员"
                            2 -> "VIP用户"
                            else -> "普通用户"
                        }
                    } ?: run {
                        binding.tvUsername.text = "未登录"
                        binding.tvRole.text = "请先登录"
                    }

                    state.deviceId?.let { deviceId ->
                        binding.tvDeviceId.text = "设备ID: $deviceId"
                    } ?: run {
                        binding.tvDeviceId.text = "设备ID: -"
                    }

                    binding.tvCacheSize.text = state.cacheSize

                    if (state.isLoggedOut) {
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
