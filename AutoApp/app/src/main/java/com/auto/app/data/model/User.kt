package com.auto.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val userId: Int,
    val username: String,
    val role: Int,
    val deviceId: String? = null,
    val expireTime: String? = null,
    val status: Int = 1
) : Parcelable {
    
    fun isAdmin(): Boolean = role == 9
    
    fun isVip(): Boolean = role == 2
    
    fun isNormal(): Boolean = role == 1
    
    fun isActive(): Boolean = status == 1
}
