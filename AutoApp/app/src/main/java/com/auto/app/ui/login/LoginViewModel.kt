package com.auto.app.ui.login

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
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AppRepository,
    application: Application
) : AndroidViewModel(application) {
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
    
    private var username: String = ""
    private var password: String = ""
    
    fun setUsername(username: String) {
        this.username = username
    }
    
    fun setPassword(password: String) {
        this.password = password
    }
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            val deviceId = repository.getDeviceId()
                ?: DeviceUtils.getDeviceId(getApplication())
            
            repository.saveDeviceId(deviceId)
            
            val result = repository.login(username, password, deviceId)
            
            result.fold(
                onSuccess = { user ->
                    _loginState.value = LoginState.Success(user.username)
                },
                onFailure = { error ->
                    _loginState.value = LoginState.Error(error.message ?: "登录失败")
                }
            )
        }
    }
    
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val username: String) : LoginState()
    data class Error(val message: String) : LoginState()
}
