package com.example.zim.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LogEntry represents a single log entry with timestamp, tag, message, and type
 */
data class LogEntry(
    val timestamp: String,
    val tag: String,
    val message: String,
    val type: LogType
)

/**
 * LogType enum for different types of logs
 */
enum class LogType {
    DEBUG,
    ERROR,
    INFO,
    WARNING
}

/**
 * Logger is responsible for storing logs and displaying them
 * It can be injected using Dagger Hilt and maintains logs in a StateFlow
 */
@Singleton
class Logger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Add a log entry
     * @param tag Log tag
     * @param message Log message
     * @param type LogType (DEBUG, ERROR, INFO, WARNING)
     * @param showToast Whether to show a toast with the message
     */
    fun addLog(
        tag: String,
        message: String,
        type: LogType = LogType.DEBUG,
        showToast: Boolean = false
    ) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val logEntry = LogEntry(timestamp, tag, message, type)

        // Add log to state
        _logs.update { currentLogs ->
            currentLogs + logEntry
        }

        // Print log based on type
        when (type) {
            LogType.DEBUG -> Log.d(tag, message)
            LogType.ERROR -> Log.e(tag, message)
            LogType.INFO -> Log.i(tag, message)
            LogType.WARNING -> Log.w(tag, message)
        }

        // Show toast if requested
        if (showToast) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Clear all logs
     */
    fun clearLogs() {
        _logs.value = emptyList()
    }

    /**
     * Get logs filtered by type
     * @param type LogType to filter by
     * @return List of filtered log entries
     */
    fun getLogsByType(type: LogType): List<LogEntry> {
        return logs.value.filter { it.type == type }
    }

    /**
     * Get logs filtered by tag
     * @param tag Tag to filter by
     * @return List of filtered log entries
     */
    fun getLogsByTag(tag: String): List<LogEntry> {
        return logs.value.filter { it.tag == tag }
    }

    /**
     * Get the most recent logs
     * @param count Number of recent logs to get
     * @return List of recent log entries
     */
    fun getRecentLogs(count: Int): List<LogEntry> {
        return logs.value.takeLast(count.coerceAtMost(logs.value.size))
    }
}