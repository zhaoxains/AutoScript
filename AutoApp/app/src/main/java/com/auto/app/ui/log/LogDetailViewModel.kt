package com.auto.app.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auto.app.data.model.ExecutionLog
import com.auto.app.data.model.TaskRecord
import com.auto.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogDetailViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {
    
    private val _record = MutableStateFlow<TaskRecord?>(null)
    val record: StateFlow<TaskRecord?> = _record.asStateFlow()
    
    private val _logs = MutableStateFlow<List<ExecutionLog>>(emptyList())
    val logs: StateFlow<List<ExecutionLog>> = _logs.asStateFlow()
    
    fun loadRecord(recordId: Long) {
        viewModelScope.launch {
            val record = repository.getRecordById(recordId)
            _record.value = record
            
            repository.getLogsByTaskId(recordId).collect { logs ->
                _logs.value = logs
            }
        }
    }
}
