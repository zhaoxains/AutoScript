package com.auto.app.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.auto.app.data.model.User
import com.auto.app.data.repository.AppRepository
import com.auto.app.service.HeartbeatService
import com.auto.app.service.ScriptScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ProfileState(
    val user: User? = null,
    val deviceId: String? = null,
    val cacheSize: String = "0 MB",
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: AppRepository,
    private val scriptScheduler: ScriptScheduler,
    application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadUserInfo()
        calculateCacheSize()
    }

    private fun loadUserInfo() {
        val user = repository.getUser()
        val deviceId = repository.getDeviceId()
        _state.value = _state.value.copy(
            user = user,
            deviceId = deviceId
        )
    }

    private fun calculateCacheSize() {
        viewModelScope.launch {
            try {
                val cacheDir = getApplication<Application>().cacheDir
                val size = getFolderSize(cacheDir)
                val sizeStr = formatFileSize(size)
                _state.value = _state.value.copy(cacheSize = sizeStr)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                val cacheDir = getApplication<Application>().cacheDir
                deleteDir(cacheDir)
                calculateCacheSize()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            scriptScheduler.reset()
            HeartbeatService.stop(getApplication())
            repository.logout()
            _state.value = _state.value.copy(isLoggedOut = true)
        }
    }

    private fun getFolderSize(folder: File): Long {
        var size: Long = 0
        folder.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                getFolderSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024))
            else -> String.format("%.1f GB", size / (1024.0 * 1024 * 1024))
        }
    }

    private fun deleteDir(dir: File?) {
        dir?.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deleteDir(file)
            } else {
                file.delete()
            }
        }
    }
}
