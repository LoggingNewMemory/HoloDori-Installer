package com.kanagawa.yamada.holodoriinstaller

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import rikka.shizuku.Shizuku

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

    private val downloadFile = File(context.filesDir, "HoloDori.apk")
    val downloader = Downloader(downloadFile)

    private val _installStatus = MutableStateFlow("")
    val installStatus = _installStatus.asStateFlow()

    private val _isInstalling = MutableStateFlow(false)
    val isInstalling = _isInstalling.asStateFlow()

    private val _useRoot = MutableStateFlow(false)
    val useRoot = _useRoot.asStateFlow()

    private val _shizukuAvailable = MutableStateFlow(false)
    val shizukuAvailable = _shizukuAvailable.asStateFlow()

    init {
        checkShizuku()
    }

    fun checkShizuku() {
        _shizukuAvailable.value = Shizuku.pingBinder()
    }

    fun setUseRoot(useRoot: Boolean) {
        _useRoot.value = useRoot
    }

    fun startApkPureDownload() {
        val apkPureUrl = "https://d.apkpure.com/b/APK/game.qualiarts.hololive.dreams.com?version=latest"
        downloader.startDownload(apkPureUrl)
    }

    fun pauseDownload() {
        downloader.pause()
    }

    fun resumeDownload() {
        startApkPureDownload()
    }

    fun cancelDownload() {
        downloader.cancel()
    }

    fun installDownloadedFile() {
        if (!downloadFile.exists()) {
            _installStatus.value = "Downloaded file not found!"
            return
        }
        performInstall(downloadFile = downloadFile, uri = null)
    }

    fun installLocalFile(uri: Uri) {
        performInstall(downloadFile = null, uri = uri)
    }

    private fun performInstall(downloadFile: File?, uri: Uri?) {
        _isInstalling.value = true
        _installStatus.value = "Starting installation..."
        
        viewModelScope.launch {
            val isRoot = _useRoot.value
            val success = if (downloadFile != null) {
                Installer.installFile(context, downloadFile, isRoot) { status ->
                    _installStatus.value = status
                }
            } else if (uri != null) {
                Installer.installFile(context, uri, isRoot) { status ->
                    _installStatus.value = status
                }
            } else {
                false
            }
            
            if (success) {
                _installStatus.value = "Installation completed successfully!"
            }
            _isInstalling.value = false
        }
    }
}
