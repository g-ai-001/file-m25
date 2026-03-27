package app.file_m25.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object Logger {
    private const val TAG = "FileManager"
    private const val LOG_FILE_NAME = "file_manager.log"
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024L // 5MB
    private const val MAX_LOG_FILES = 3

    private lateinit var logDir: File
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        logDir = context.getExternalFilesDir(null) ?: context.filesDir
        isInitialized = true
        d(TAG, "Logger initialized, log directory: ${logDir.absolutePath}")
    }

    fun d(tag: String, message: String) {
        log("DEBUG", tag, message)
    }

    fun i(tag: String, message: String) {
        log("INFO", tag, message)
    }

    fun w(tag: String, message: String) {
        log("WARN", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log("ERROR", tag, message)
        throwable?.let {
            log("ERROR", tag, "Exception: ${it.message}")
            it.stackTrace.forEach { element ->
                log("ERROR", tag, "  at $element")
            }
        }
    }

    private fun log(level: String, tag: String, message: String) {
        if (!isInitialized) return
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] [$level] [$tag] $message"
        logQueue.offer(logEntry)
        executor.execute {
            flushLogs()
        }
    }

    private fun flushLogs() {
        val logs = mutableListOf<String>()
        while (true) {
            val log = logQueue.poll() ?: break
            logs.add(log)
        }
        if (logs.isEmpty()) return

        val logFile = File(logDir, LOG_FILE_NAME)
        try {
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                rotateLogs()
            }
            FileWriter(logFile, true).use { writer ->
                logs.forEach { writer.write("$it\n") }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun rotateLogs() {
        for (i in MAX_LOG_FILES - 1 downTo 1) {
            val oldFile = File(logDir, "$LOG_FILE_NAME.$i")
            val newFile = File(logDir, "$LOG_FILE_NAME.${i + 1}")
            if (oldFile.exists()) {
                oldFile.renameTo(newFile)
            }
        }
        val currentLog = File(logDir, LOG_FILE_NAME)
        if (currentLog.exists()) {
            currentLog.renameTo(File(logDir, "$LOG_FILE_NAME.1"))
        }
    }

    fun getLogFile(): File? {
        return if (isInitialized) File(logDir, LOG_FILE_NAME) else null
    }
}