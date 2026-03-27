package app.file_m25.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkUpdateManager @Inject constructor() {

    companion object {
        private const val TAG = "ApkUpdateManager"
    }

    fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "未知"
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get current version", e)
            "未知"
        }
    }

    fun getCurrentVersionCode(context: Context): Long {
        return try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get current version code", e)
            0L
        }
    }

    fun parseApkInfo(context: Context, apkFile: File): ApkInfo? {
        if (!apkFile.exists() || !apkFile.name.endsWith(".apk", ignoreCase = true)) {
            return null
        }

        return try {
            val packageManager = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            }

            if (packageInfo == null) {
                Logger.e(TAG, "Failed to parse APK info: ${apkFile.absolutePath}")
                return null
            }

            val appName = packageInfo.applicationInfo?.let {
                packageManager.getApplicationLabel(it).toString()
            } ?: "未知应用"

            val versionName = packageInfo.versionName ?: "未知"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            val packageName = packageInfo.packageName

            ApkInfo(
                packageName = packageName,
                appName = appName,
                versionName = versionName,
                versionCode = versionCode,
                filePath = apkFile.absolutePath,
                fileSize = apkFile.length()
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to parse APK info", e)
            null
        }
    }

    fun isNewerVersion(context: Context, apkInfo: ApkInfo): Boolean {
        val currentVersionCode = getCurrentVersionCode(context)
        return apkInfo.versionCode > currentVersionCode
    }

    fun isSameApp(context: Context, apkInfo: ApkInfo): Boolean {
        return apkInfo.packageName == context.packageName
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) {
            Logger.e(TAG, "APK file does not exist: ${apkFile.absolutePath}")
            return false
        }

        return try {
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Logger.i(TAG, "Install intent started for: ${apkFile.absolutePath}")
            true
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to install APK", e)
            false
        }
    }

    data class ApkInfo(
        val packageName: String,
        val appName: String,
        val versionName: String,
        val versionCode: Long,
        val filePath: String,
        val fileSize: Long
    )
}