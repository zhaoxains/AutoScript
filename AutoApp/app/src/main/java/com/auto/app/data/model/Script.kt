package com.auto.app.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Script(
    @SerializedName("script_id")
    val scriptId: Int,
    
    @SerializedName("script_name")
    val scriptName: String,
    
    @SerializedName("script_desc")
    val scriptDesc: String? = null,
    
    @SerializedName("target_package")
    val targetPackage: String,
    
    @SerializedName("config")
    val config: ScriptConfig,
    
    @SerializedName("version")
    val version: String = "1.0.0",
    
    @SerializedName("status")
    val status: Int = 0,
    
    @SerializedName("priority")
    val priority: Int = 2,
    
    @SerializedName("execution_frequency")
    val executionFrequency: String = "once",
    
    @SerializedName("timeout")
    val timeout: Int = 300
) : Parcelable {
    
    fun isOnline(): Boolean = status == 1
    
    fun isHighPriority(): Boolean = priority == 1
}

@Parcelize
data class ScriptConfig(
    @SerializedName("controls")
    val controls: List<ControlConfig>? = null,
    
    @SerializedName("init_controls")
    val initControls: List<ControlConfig>? = null,
    
    @SerializedName("ad_config")
    val adConfig: AdConfig? = null,
    
    @SerializedName("conditions")
    val conditions: List<ConditionConfig>? = null,
    
    @SerializedName("loop_config")
    val loopConfig: LoopConfig? = null,
    
    @SerializedName("record_config")
    val recordConfig: RecordConfig? = null
) : Parcelable

@Parcelize
data class ControlConfig(
    @SerializedName("control_id")
    val controlId: String,
    
    @SerializedName("control_name")
    val controlName: String,
    
    @SerializedName("match_type")
    val matchType: String,
    
    @SerializedName("match_value")
    val matchValue: String = "",
    
    @SerializedName("match_mode")
    val matchMode: String = "exact",
    
    @SerializedName("operation")
    val operation: String,
    
    @SerializedName("delay_before")
    val delayBefore: Long = 1000,
    
    @SerializedName("delay_after")
    val delayAfter: Long = 500,
    
    @SerializedName("retry_count")
    val retryCount: Int = 3,
    
    @SerializedName("position")
    val position: PositionConfig? = null,
    
    @SerializedName("wait_time")
    val waitTime: Long? = null,
    
    @SerializedName("input_text")
    val inputText: String? = null,
    
    @SerializedName("clear_before_input")
    val clearBeforeInput: Boolean = false,
    
    @SerializedName("swipe_config")
    val swipeConfig: SwipeConfig? = null,
    
    @SerializedName("icon_config")
    val iconConfig: IconConfig? = null,
    
    @SerializedName("fallback_strategies")
    val fallbackStrategies: List<FallbackStrategy>? = null,
    
    @SerializedName("timeout")
    val timeout: Long? = null,
    
    @SerializedName("check_interval")
    val checkInterval: Long? = null,
    
    @SerializedName("long_press_duration")
    val longPressDuration: Long = 500,
    
    @SerializedName("double_click_interval")
    val doubleClickInterval: Long = 100,
    
    @SerializedName("multi_language_values")
    val multiLanguageValues: List<String>? = null,
    
    @SerializedName("scroll_on_not_found")
    val scrollOnNotFound: ScrollConfig? = null,
    
    @SerializedName("batch_config")
    val batchConfig: BatchConfig? = null,
    
    @SerializedName("popup_configs")
    val popupConfigs: List<PopupConfig>? = null,
    
    @SerializedName("navigation_type")
    val navigationType: String? = null
) : Parcelable

@Parcelize
data class IconConfig(
    @SerializedName("content_description")
    val contentDescription: String? = null,
    
    @SerializedName("view_id")
    val viewId: String? = null,
    
    @SerializedName("class_name")
    val className: String? = null,
    
    @SerializedName("clickable")
    val clickable: Boolean? = null,
    
    @SerializedName("bounds_hint")
    val boundsHint: BoundsHint? = null,
    
    @SerializedName("parent_text")
    val parentText: String? = null,
    
    @SerializedName("sibling_text")
    val siblingText: String? = null,
    
    @SerializedName("position_hint")
    val positionHint: String? = null
) : Parcelable

@Parcelize
data class BoundsHint(
    @SerializedName("x_ratio_min")
    val xRatioMin: Float = 0f,
    
    @SerializedName("x_ratio_max")
    val xRatioMax: Float = 1f,
    
    @SerializedName("y_ratio_min")
    val yRatioMin: Float = 0f,
    
    @SerializedName("y_ratio_max")
    val yRatioMax: Float = 1f
) : Parcelable

@Parcelize
data class FallbackStrategy(
    @SerializedName("strategy_type")
    val strategyType: String,
    
    @SerializedName("strategy_value")
    val strategyValue: String = "",
    
    @SerializedName("position")
    val position: PositionConfig? = null
) : Parcelable

@Parcelize
data class SwipeConfig(
    @SerializedName("start_x_ratio")
    val startXRatio: Float = 0.5f,
    
    @SerializedName("start_y_ratio")
    val startYRatio: Float = 0.7f,
    
    @SerializedName("end_x_ratio")
    val endXRatio: Float = 0.5f,
    
    @SerializedName("end_y_ratio")
    val endYRatio: Float = 0.3f,
    
    @SerializedName("duration")
    val duration: Int = 500
) : Parcelable

/**
 * 位置配置
 * 支持相对节点定位：通过 relative_to 指定参考节点，然后在相对位置点击
 */
@Parcelize
data class PositionConfig(
    @SerializedName("x_ratio")
    val xRatio: Float,
    
    @SerializedName("y_ratio")
    val yRatio: Float,
    
    @SerializedName("relative_to")
    val relativeTo: String? = null,
    
    @SerializedName("offset_x_ratio")
    val offsetXRatio: Float = 0f,
    
    @SerializedName("offset_y_ratio")
    val offsetYRatio: Float = 0f
) : Parcelable

@Parcelize
data class AdConfig(
    @SerializedName("ad_keywords")
    val adKeywords: List<String>? = null,
    
    @SerializedName("duration_regex")
    val durationRegex: String? = null,
    
    @SerializedName("close_keywords")
    val closeKeywords: List<String>? = null,
    
    @SerializedName("close_position")
    val closePosition: List<String>? = null,
    
    @SerializedName("default_wait")
    val defaultWait: Int = 15
) : Parcelable

@Parcelize
data class ConditionConfig(
    @SerializedName("condition_id")
    val conditionId: String,
    
    @SerializedName("condition_type")
    val conditionType: String,
    
    @SerializedName("condition_value")
    val conditionValue: String,
    
    @SerializedName("true_branch")
    val trueBranch: List<String>? = null,
    
    @SerializedName("false_branch")
    val falseBranch: List<String>? = null
) : Parcelable

@Parcelize
data class LoopConfig(
    @SerializedName("loop_type")
    val loopType: String,
    
    @SerializedName("loop_count")
    val loopCount: Int = 1,
    
    @SerializedName("loop_interval")
    val loopInterval: Long = 2000,
    
    @SerializedName("exit_condition")
    val exitCondition: ConditionConfig? = null
) : Parcelable

@Parcelize
data class RecordConfig(
    @SerializedName("base_fields")
    val baseFields: List<String>? = null,
    
    @SerializedName("extend_fields")
    val extendFields: List<ExtendField>? = null
) : Parcelable

@Parcelize
data class ExtendField(
    @SerializedName("field_name")
    val fieldName: String,
    
    @SerializedName("field_type")
    val fieldType: String,
    
    @SerializedName("field_desc")
    val fieldDesc: String? = null
) : Parcelable

@Parcelize
data class ScrollConfig(
    @SerializedName("direction")
    val direction: String = "up",
    
    @SerializedName("distance_ratio")
    val distanceRatio: Float = 0.3f,
    
    @SerializedName("max_scrolls")
    val maxScrolls: Int = 3
) : Parcelable

@Parcelize
data class BatchConfig(
    @SerializedName("batch_count")
    val batchCount: Int = 1,
    
    @SerializedName("scroll_after_each")
    val scrollAfterEach: Boolean = true,
    
    @SerializedName("scroll_config")
    val scrollConfig: SwipeConfig? = null
) : Parcelable

@Parcelize
data class PopupConfig(
    @SerializedName("popup_id")
    val popupId: String,
    
    @SerializedName("match_type")
    val matchType: String,
    
    @SerializedName("match_value")
    val matchValue: String,
    
    @SerializedName("close_match_type")
    val closeMatchType: String = "text",
    
    @SerializedName("close_match_value")
    val closeMatchValue: String,
    
    @SerializedName("priority")
    val priority: Int = 1
) : Parcelable
