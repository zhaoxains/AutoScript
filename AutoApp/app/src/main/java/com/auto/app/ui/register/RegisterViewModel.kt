package com.auto.app.ui.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.auto.app.data.repository.AppRepository
import com.auto.app.util.DeviceUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: AppRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    fun register(username: String, password: String, confirmPassword: String, referrer: String = "") {
        if (!validateInput(username, password, confirmPassword)) {
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            val result = repository.register(username, password, referrer)
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(isLoading = false, isSuccess = true)
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(isLoading = false, error = error.message)
                }
            )
        }
    }

    private fun validateInput(username: String, password: String, confirmPassword: String): Boolean {
        if (username.isBlank()) {
            _state.value = _state.value.copy(error = "请输入用户名")
            return false
        }
        if (username.length < 3) {
            _state.value = _state.value.copy(error = "用户名至少需要3个字符")
            return false
        }
        if (password.isBlank()) {
            _state.value = _state.value.copy(error = "请输入密码")
            return false
        }
        if (password.length < 6) {
            _state.value = _state.value.copy(error = "密码至少需要6个字符")
            return false
        }
        if (password != confirmPassword) {
            _state.value = _state.value.copy(error = "两次密码输入不一致")
            return false
        }
        return true
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
