package com.auto.app.data.local.prefs

import android.content.Context
import com.auto.app.data.model.GlobalConfig
import com.auto.app.data.model.Script
import com.auto.app.data.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    context: Context
) {
    private val mmkv = MMKV.defaultMMKV()
    private val gson = Gson()
    
    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_INFO = "user_info"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_SCRIPTS = "scripts"
        private const val KEY_SCRIPTS_VERSION = "scripts_version"
        private const val KEY_GLOBAL_CONFIG = "global_config"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_ACCESSIBILITY_ENABLED = "accessibility_enabled"
        private const val KEY_FLOAT_WINDOW_ENABLED = "float_window_enabled"
        private const val KEY_LAST_HEARTBEAT = "last_heartbeat"
    }
    
    fun saveToken(token: String) {
        mmkv.encode(KEY_TOKEN, token)
    }
    
    fun getToken(): String? {
        return mmkv.decodeString(KEY_TOKEN, null)
    }
    
    fun clearToken() {
        mmkv.remove(KEY_TOKEN)
    }
    
    fun saveUser(user: User) {
        mmkv.encode(KEY_USER_INFO, gson.toJson(user))
    }
    
    fun getUser(): User? {
        val json = mmkv.decodeString(KEY_USER_INFO, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun clearUser() {
        mmkv.remove(KEY_USER_INFO)
    }
    
    fun getDeviceId(): String? {
        return mmkv.decodeString(KEY_DEVICE_ID, null)
    }
    
    fun saveDeviceId(deviceId: String) {
        mmkv.encode(KEY_DEVICE_ID, deviceId)
    }
    
    fun setLoggedIn(loggedIn: Boolean) {
        mmkv.encode(KEY_IS_LOGGED_IN, loggedIn)
    }
    
    fun isLoggedIn(): Boolean {
        return mmkv.decodeBool(KEY_IS_LOGGED_IN, false)
    }
    
    fun saveScripts(scripts: List<Script>) {
        mmkv.encode(KEY_SCRIPTS, gson.toJson(scripts))
    }
    
    fun getScripts(): List<Script> {
        val json = mmkv.decodeString(KEY_SCRIPTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Script>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun saveScriptsVersion(version: String) {
        mmkv.encode(KEY_SCRIPTS_VERSION, version)
    }
    
    fun getScriptsVersion(): String? {
        return mmkv.decodeString(KEY_SCRIPTS_VERSION, null)
    }
    
    fun saveGlobalConfig(config: GlobalConfig) {
        mmkv.encode(KEY_GLOBAL_CONFIG, gson.toJson(config))
    }
    
    fun getGlobalConfig(): GlobalConfig? {
        val json = mmkv.decodeString(KEY_GLOBAL_CONFIG, null) ?: return null
        return try {
            gson.fromJson(json, GlobalConfig::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun isFirstLaunch(): Boolean {
        return mmkv.decodeBool(KEY_FIRST_LAUNCH, true)
    }
    
    fun setFirstLaunch(first: Boolean) {
        mmkv.encode(KEY_FIRST_LAUNCH, first)
    }
    
    fun isAccessibilityEnabled(): Boolean {
        return mmkv.decodeBool(KEY_ACCESSIBILITY_ENABLED, false)
    }
    
    fun setAccessibilityEnabled(enabled: Boolean) {
        mmkv.encode(KEY_ACCESSIBILITY_ENABLED, enabled)
    }
    
    fun isFloatWindowEnabled(): Boolean {
        return mmkv.decodeBool(KEY_FLOAT_WINDOW_ENABLED, false)
    }
    
    fun setFloatWindowEnabled(enabled: Boolean) {
        mmkv.encode(KEY_FLOAT_WINDOW_ENABLED, enabled)
    }
    
    fun saveLastHeartbeat(timestamp: Long) {
        mmkv.encode(KEY_LAST_HEARTBEAT, timestamp)
    }
    
    fun getLastHeartbeat(): Long {
        return mmkv.decodeLong(KEY_LAST_HEARTBEAT, 0)
    }
    
    fun clearAll() {
        mmkv.clearAll()
    }
    
    fun clearLoginInfo() {
        mmkv.remove(KEY_TOKEN)
        mmkv.remove(KEY_USER_INFO)
        mmkv.remove(KEY_IS_LOGGED_IN)
    }
}
