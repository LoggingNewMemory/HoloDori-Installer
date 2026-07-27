package com.kanagawa.yamada.holodoriinstaller

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLDecoder
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

    private val _rootAvailable = MutableStateFlow(false)
    val rootAvailable = _rootAvailable.asStateFlow()

    // Version info
    private val _installedVersion = MutableStateFlow<String?>(null)
    val installedVersion = _installedVersion.asStateFlow()

    private val _latestVersion = MutableStateFlow<String?>(null)
    val latestVersion = _latestVersion.asStateFlow()

    private val _isCheckingVersion = MutableStateFlow(false)
    val isCheckingVersion = _isCheckingVersion.asStateFlow()

    companion object {
        const val PACKAGE_NAME = "game.qualiarts.hololive.dreams.com"
        private const val APKPURE_URL = "https://d.apkpure.com/b/APK/$PACKAGE_NAME?version=latest"
    }

    init {
        checkShizuku()
        checkRoot()
        refreshVersionInfo()
    }

    fun checkShizuku() {
        _shizukuAvailable.value = try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    fun checkRoot() {
        viewModelScope.launch(Dispatchers.IO) {
            _rootAvailable.value = try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                val exitCode = process.waitFor()
                exitCode == 0
            } catch (_: Exception) {
                false
            }
        }
    }

    fun setUseRoot(useRoot: Boolean) {
        _useRoot.value = useRoot
    }

    fun refreshVersionInfo() {
        checkInstalledVersion()
        checkLatestVersion()
    }

    private fun checkInstalledVersion() {
        _installedVersion.value = try {
            val info = context.packageManager.getPackageInfo(PACKAGE_NAME, 0)
            info.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun checkLatestVersion() {
        _isCheckingVersion.value = true
        viewModelScope.launch {
            _latestVersion.value = fetchLatestVersionFromApkPure()
            _isCheckingVersion.value = false
        }
    }

    private suspend fun fetchLatestVersionFromApkPure(): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()

            val request = Request.Builder()
                .url(APKPURE_URL)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                .head()
                .build()

            val response = client.newCall(request).execute()
            val location = response.header("Location") ?: return@withContext null

            // Parse version from filename param: ...&filename=hololive+Dreams_1.0.0_APKPure.apk&...
            val filenameParam = Regex("[?&]filename=([^&]+)").find(location)?.groupValues?.get(1)
            if (filenameParam != null) {
                val decoded = URLDecoder.decode(filenameParam, "UTF-8")
                // Format: "hololive Dreams_VERSION_APKPure.apk"
                val parts = decoded.removeSuffix(".apk").split("_")
                if (parts.size >= 2) {
                    return@withContext parts[parts.size - 2] // version is second to last
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun startApkPureDownload(forceRestart: Boolean = true) {
        downloader.startDownload(APKPURE_URL, forceRestart = forceRestart)
    }

    fun pauseDownload() {
        downloader.pause()
    }

    fun resumeDownload() {
        startApkPureDownload(forceRestart = false)
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
                // Refresh installed version after successful install
                checkInstalledVersion()
            }
            _isInstalling.value = false
        }
    }
}
