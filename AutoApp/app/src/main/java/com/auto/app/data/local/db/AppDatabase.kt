package com.auto.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.auto.app.data.model.ExecutionLog
import com.auto.app.data.model.TaskRecord

@Database(
    entities = [
        TaskRecord::class,
        ExecutionLog::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskRecordDao(): TaskRecordDao
    abstract fun executionLogDao(): ExecutionLogDao
}
