package app.file_m25.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.file_m25.BuildConfig
import app.file_m25.data.repository.PreferencesRepository
import app.file_m25.util.formatFileSize

private val themeColors = listOf(
    0xFF0061A4.toInt(),
    0xFF6200EE.toInt(),
    0xFF3700B3.toInt(),
    0xFF03DAC5.toInt(),
    0xFF018786.toInt(),
    0xFFB00020.toInt(),
    0xFFCF6679.toInt(),
    0xFF000000.toInt()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "外观",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("主题") },
                supportingContent = { Text(getThemeModeLabel(uiState.themeMode)) },
                leadingContent = {
                    Icon(
                        when (uiState.themeMode) {
                            PreferencesRepository.ThemeMode.LIGHT -> Icons.Default.LightMode
                            PreferencesRepository.ThemeMode.DARK -> Icons.Default.DarkMode
                            PreferencesRepository.ThemeMode.SYSTEM -> Icons.Default.PhoneAndroid
                        },
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { viewModel.showThemeDialog() }
            )

            ListItem(
                headlineContent = { Text("动态颜色") },
                supportingContent = { Text(if (uiState.dynamicColorEnabled) "已启用" else "已禁用") },
                leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiState.dynamicColorEnabled,
                        onCheckedChange = { viewModel.setDynamicColorEnabled(it) }
                    )
                }
            )

            Text(
                text = "主题颜色",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(themeColors) { color ->
                    ColorOption(
                        color = color,
                        isSelected = uiState.primaryColor == color,
                        onClick = {
                            viewModel.setPrimaryColor(
                                if (uiState.primaryColor == color) null else color
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "更新",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("检查更新") },
                supportingContent = { Text("当前版本: ${uiState.currentVersion}") },
                leadingContent = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
                modifier = Modifier.clickable { viewModel.scanForUpdateApk() }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "关于",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("应用名称") },
                supportingContent = { Text("文件管理器") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
            )

            ListItem(
                headlineContent = { Text("版本") },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "日志",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("日志文件路径") },
                supportingContent = {
                    Text(
                        text = uiState.logFilePath.ifEmpty { "暂无日志" },
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) }
            )

            ListItem(
                headlineContent = { Text("日志文件大小") },
                supportingContent = { Text(uiState.logFileSize.ifEmpty { "0 B" }) },
                leadingContent = { Icon(Icons.Default.BugReport, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "日志文件保存在应用的外部存储目录中，当遇到问题时可以通过日志文件分析问题原因。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    if (uiState.showThemeDialog) {
        ThemeDialog(
            currentMode = uiState.themeMode,
            onModeSelected = {
                viewModel.setThemeMode(it)
                viewModel.hideThemeDialog()
            },
            onDismiss = { viewModel.hideThemeDialog() }
        )
    }

    if (uiState.showUpdateDialog && uiState.apkInfo != null) {
        UpdateDialog(
            apkInfo = uiState.apkInfo!!,
            onInstall = {
                viewModel.installUpdate()
                viewModel.hideUpdateDialog()
            },
            onDismiss = { viewModel.hideUpdateDialog() }
        )
    }
}

@Composable
private fun ColorOption(
    color: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(color))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "已选中",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ThemeDialog(
    currentMode: PreferencesRepository.ThemeMode,
    onModeSelected: (PreferencesRepository.ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题") },
        text = {
            Column {
                ThemeModeOption(
                    title = "跟随系统",
                    icon = Icons.Default.PhoneAndroid,
                    isSelected = currentMode == PreferencesRepository.ThemeMode.SYSTEM,
                    onClick = { onModeSelected(PreferencesRepository.ThemeMode.SYSTEM) }
                )
                ThemeModeOption(
                    title = "浅色",
                    icon = Icons.Default.LightMode,
                    isSelected = currentMode == PreferencesRepository.ThemeMode.LIGHT,
                    onClick = { onModeSelected(PreferencesRepository.ThemeMode.LIGHT) }
                )
                ThemeModeOption(
                    title = "深色",
                    icon = Icons.Default.DarkMode,
                    isSelected = currentMode == PreferencesRepository.ThemeMode.DARK,
                    onClick = { onModeSelected(PreferencesRepository.ThemeMode.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun UpdateDialog(
    apkInfo: app.file_m25.util.ApkUpdateManager.ApkInfo,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本") },
        text = {
            Column {
                Text("应用名称: ${apkInfo.appName}")
                Text("版本: ${apkInfo.versionName}")
                Text("文件大小: ${formatFileSize(apkInfo.fileSize)}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "是否安装此更新？",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onInstall) {
                Text("安装")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ThemeModeOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f))
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
    }
}

private fun getThemeModeLabel(mode: PreferencesRepository.ThemeMode): String {
    return when (mode) {
        PreferencesRepository.ThemeMode.SYSTEM -> "跟随系统"
        PreferencesRepository.ThemeMode.LIGHT -> "浅色"
        PreferencesRepository.ThemeMode.DARK -> "深色"
    }
}