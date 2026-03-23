package com.auto.app.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GlobalConfig(
    @SerializedName("operation_delay_min")
    val operationDelayMin: Long = 500,
    
    @SerializedName("operation_delay_max")
    val operationDelayMax: Long = 2000,
    
    @SerializedName("retry_count")
    val retryCount: Int = 3,
    
    @SerializedName("retry_interval")
    val retryInterval: Long = 1000,
    
    @SerializedName("ad_default_duration")
    val adDefaultDuration: Int = 15,
    
    @SerializedName("page_load_timeout")
    val pageLoadTimeout: Int = 10,
    
    @SerializedName("task_max_duration")
    val taskMaxDuration: Long = 7200,
    
    @SerializedName("daily_task_limit")
    val dailyTaskLimit: Int = 100
) : Parcelable
