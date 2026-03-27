package app.file_m25.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.file_m25.data.repository.PreferencesRepository
import app.file_m25.util.ApkUpdateManager
import app.file_m25.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val logFilePath: String = "",
    val logFileSize: String = "",
    val themeMode: PreferencesRepository.ThemeMode = PreferencesRepository.ThemeMode.SYSTEM,
    val primaryColor: Int? = null,
    val dynamicColorEnabled: Boolean = true,
    val showThemeDialog: Boolean = false,
    val currentVersion: String = "",
    val showUpdateDialog: Boolean = false,
    val apkInfo: ApkUpdateManager.ApkInfo? = null,
    val snackbarMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val apkUpdateManager = ApkUpdateManager()

    init {
        loadLogInfo()
        loadThemeSettings()
        loadVersionInfo()
    }

    private fun loadLogInfo() {
        viewModelScope.launch {
            val logFile = app.file_m25.util.Logger.getLogFile()
            if (logFile != null && logFile.exists()) {
                val size = logFile.length()
                val sizeStr = when {
                    size < 1024 -> "$size B"
                    size < 1024 * 1024 -> "%.1f KB".format(size / 1024.0)
                    else -> "%.1f MB".format(size / (1024.0 * 1024.0))
                }
                _uiState.update {
                    it.copy(
                        logFilePath = logFile.absolutePath,
                        logFileSize = sizeStr
                    )
                }
            }
        }
    }

    private fun loadThemeSettings() {
        viewModelScope.launch {
            preferencesRepository.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.primaryColor.collect { color ->
                _uiState.update { it.copy(primaryColor = color) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.dynamicColorEnabled.collect { enabled ->
                _uiState.update { it.copy(dynamicColorEnabled = enabled) }
            }
        }
    }

    private fun loadVersionInfo() {
        val version = apkUpdateManager.getCurrentVersion(context)
        _uiState.update { it.copy(currentVersion = version) }
    }

    fun setThemeMode(mode: PreferencesRepository.ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
            Logger.i("SettingsViewModel", "Theme mode set to: $mode")
        }
    }

    fun setPrimaryColor(color: Int?) {
        viewModelScope.launch {
            preferencesRepository.setPrimaryColor(color)
            Logger.i("SettingsViewModel", "Primary color set to: ${color?.let { String.format("#%06X", 0xFFFFFF and it) }}")
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDynamicColorEnabled(enabled)
            Logger.i("SettingsViewModel", "Dynamic color enabled: $enabled")
        }
    }

    fun showThemeDialog() {
        _uiState.update { it.copy(showThemeDialog = true) }
    }

    fun hideThemeDialog() {
        _uiState.update { it.copy(showThemeDialog = false) }
    }

    fun scanForUpdateApk(directory: String = "/storage/emulated/0/Download") {
        viewModelScope.launch {
            try {
                val dir = File(directory)
                if (!dir.exists() || !dir.isDirectory) {
                    showError("目录不存在: $directory")
                    return@launch
                }

                val apkFiles = dir.listFiles()?.filter {
                    it.name.endsWith(".apk", ignoreCase = true)
                } ?: emptyList()

                if (apkFiles.isEmpty()) {
                    showError("未找到APK文件")
                    return@launch
                }

                // 查找最新版本的APK
                val validApk = apkFiles
                    .mapNotNull { apkUpdateManager.parseApkInfo(context, it) }
                    .filter { apkUpdateManager.isSameApp(context, it) }
                    .filter { apkUpdateManager.isNewerVersion(context, it) }
                    .maxByOrNull { it.versionCode }

                if (validApk != null) {
                    _uiState.update {
                        it.copy(
                            showUpdateDialog = true,
                            apkInfo = validApk
                        )
                    }
                } else {
                    showError("未找到新版本APK")
                }
            } catch (e: Exception) {
                Logger.e("SettingsViewModel", "Failed to scan for update", e)
                showError("扫描失败: ${e.message}")
            }
        }
    }

    fun installUpdate() {
        val apkInfo = _uiState.value.apkInfo ?: return
        val apkFile = File(apkInfo.filePath)

        if (!apkFile.exists()) {
            showError("APK文件不存在")
            return
        }

        val success = apkUpdateManager.installApk(context, apkFile)
        if (!success) {
            showError("启动安装失败")
        }
    }

    fun hideUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false, apkInfo = null) }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}