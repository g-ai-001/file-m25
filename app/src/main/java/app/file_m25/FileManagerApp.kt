package app.file_m25

import android.app.Application
import app.file_m25.util.Logger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FileManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.init(this)
        Logger.d("FileManagerApp", "Application started")
    }

    override fun onTerminate() {
        super.onTerminate()
        Logger.shutdown()
    }
}