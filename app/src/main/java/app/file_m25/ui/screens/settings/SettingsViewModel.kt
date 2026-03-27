package app.file_m25.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.file_m25.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val logFilePath: String = "",
    val logFileSize: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadLogInfo()
    }

    private fun loadLogInfo() {
        viewModelScope.launch {
            val logFile = Logger.getLogFile()
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
}