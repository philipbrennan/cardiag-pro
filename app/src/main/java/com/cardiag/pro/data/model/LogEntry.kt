package com.cardiag.pro.data.model

/**
 * Represents a single log entry with timestamp, level, and metadata.
 */
data class LogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Get formatted timestamp string.
     */
    fun getFormattedTime(): String {
        val date = java.util.Date(timestamp)
        return java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(date)
    }

    /**
     * Get full formatted log line.
     */
    fun toLogLine(): String {
        val date = java.util.Date(timestamp)
        val formattedTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(date)
        val builder = StringBuilder()
        builder.append("[$formattedTime] ${level.displayName}/$tag: $message")

        if (metadata.isNotEmpty()) {
            builder.append(" | ")
            builder.append(metadata.entries.joinToString(", ") { "${it.key}=${it.value}" })
        }

        throwable?.let {
            builder.append("\n")
            builder.append(it.stackTraceToString())
        }

        return builder.toString()
    }
}

/**
 * Log severity levels.
 */
enum class LogLevel(val priority: Int, val displayName: String, val colorCode: String) {
    DEBUG(2, "DEBUG", "#9E9E9E"),      // Gray
    INFO(3, "INFO", "#2196F3"),         // Blue
    WARN(4, "WARN", "#FF9800"),         // Orange
    ERROR(5, "ERROR", "#F44336");       // Red

    companion object {
        fun fromPriority(priority: Int): LogLevel {
            return entries.find { it.priority == priority } ?: DEBUG
        }
    }
}
