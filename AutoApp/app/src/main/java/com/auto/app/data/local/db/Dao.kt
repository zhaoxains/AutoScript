package com.auto.app.data.local.db

import androidx.room.*
import com.auto.app.data.model.ExecutionLog
import com.auto.app.data.model.TaskRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskRecordDao {
    
    @Query("SELECT * FROM task_records ORDER BY createTime DESC")
    fun getAllRecords(): Flow<List<TaskRecord>>
    
    @Query("SELECT * FROM task_records WHERE status = :status ORDER BY createTime DESC")
    fun getRecordsByStatus(status: Int): Flow<List<TaskRecord>>
    
    @Query("SELECT * FROM task_records WHERE scriptId = :scriptId ORDER BY createTime DESC")
    fun getRecordsByScriptId(scriptId: Int): Flow<List<TaskRecord>>
    
    @Query("SELECT * FROM task_records WHERE startTime BETWEEN :startTime AND :endTime ORDER BY createTime DESC")
    fun getRecordsByTimeRange(startTime: Long, endTime: Long): Flow<List<TaskRecord>>
    
    @Query("SELECT * FROM task_records WHERE id = :id")
    suspend fun getRecordById(id: Long): TaskRecord?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TaskRecord): Long
    
    @Update
    suspend fun updateRecord(record: TaskRecord)
    
    @Delete
    suspend fun deleteRecord(record: TaskRecord)
    
    @Query("DELETE FROM task_records WHERE createTime < :beforeTime")
    suspend fun deleteOldRecords(beforeTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM task_records WHERE status = :status")
    suspend fun countByStatus(status: Int): Int
    
    @Query("SELECT COUNT(*) FROM task_records WHERE DATE(startTime / 1000, 'unixepoch') = DATE(:timestamp / 1000, 'unixepoch')")
    suspend fun countTodayRecords(timestamp: Long = System.currentTimeMillis()): Int
}

@Dao
interface ExecutionLogDao {
    
    @Query("SELECT * FROM execution_logs WHERE taskId = :taskId ORDER BY logTime ASC")
    fun getLogsByTaskId(taskId: Long): Flow<List<ExecutionLog>>
    
    @Query("SELECT * FROM execution_logs WHERE taskId = :taskId ORDER BY logTime ASC")
    suspend fun getLogsByTaskIdDirect(taskId: Long): List<ExecutionLog>
    
    @Query("SELECT * FROM execution_logs WHERE taskId = :taskId AND logType = :logType ORDER BY logTime DESC")
    fun getLogsByTaskIdAndType(taskId: Long, logType: String): Flow<List<ExecutionLog>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ExecutionLog): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<ExecutionLog>)
    
    @Query("DELETE FROM execution_logs WHERE logTime < :beforeTime")
    suspend fun deleteOldLogs(beforeTime: Long): Int
    
    @Query("DELETE FROM execution_logs WHERE taskId = :taskId")
    suspend fun deleteLogsByTaskId(taskId: Long)
}
