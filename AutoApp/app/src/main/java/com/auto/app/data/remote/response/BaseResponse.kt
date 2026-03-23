package com.auto.app.data.remote.response

import com.google.gson.annotations.SerializedName

data class BaseResponse<T>(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("msg")
    val msg: String,
    
    @SerializedName("data")
    val data: T? = null
) {
    fun isSuccess(): Boolean = code == 0
    
    fun isAuthFailed(): Boolean = code == 1002
    
    fun isDeviceConflict(): Boolean = code == 3003
}

object ResponseCode {
    const val SUCCESS = 0
    const val PARAM_ERROR = 1001
    const val AUTH_FAILED = 1002
    const val PERMISSION_DENIED = 1003
    const val RESOURCE_NOT_FOUND = 1004
    const val SERVER_ERROR = 2001
    const val DATABASE_ERROR = 2002
    const val DEVICE_NOT_BOUND = 3001
    const val ACCOUNT_EXPIRED = 3002
    const val DEVICE_CONFLICT = 3003
}
