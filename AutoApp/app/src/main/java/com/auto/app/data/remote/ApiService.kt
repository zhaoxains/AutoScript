package com.auto.app.data.remote

import com.auto.app.data.model.GlobalConfig
import com.auto.app.data.remote.request.*
import com.auto.app.data.remote.response.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {
    
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): BaseResponse<LoginResponse>
    
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): BaseResponse<LoginResponse>
    
    @POST("api/auth/logout")
    suspend fun logout(): BaseResponse<Unit>
    
    @GET("api/config/scripts")
    suspend fun getScripts(
        @Query("version") version: String? = null
    ): BaseResponse<ScriptsResponse>
    
    @GET("api/config/global")
    suspend fun getGlobalConfig(): BaseResponse<GlobalConfig>
    
    @POST("api/report/task")
    suspend fun reportTask(@Body request: TaskReportRequest): BaseResponse<ReportResponse>
    
    @POST("api/report/logs")
    suspend fun reportLogs(@Body request: LogReportRequest): BaseResponse<LogReportResponse>
    
    @POST("api/heartbeat")
    suspend fun heartbeat(@Body request: HeartbeatRequest): BaseResponse<HeartbeatResponse>
    
    @Multipart
    @POST("api/screenshot/upload")
    suspend fun uploadScreenshot(
        @Part screenshot: MultipartBody.Part
    ): BaseResponse<ScreenshotUploadResponse>
}
