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

    private val downloadFile = File(context.filesDir, "HoloDori.xapk")
    val downloader = Downloader(downloadFile)

    private val _installLogs = MutableStateFlow<List<String>>(emptyList())
    val installLogs = _installLogs.asStateFlow()

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
        private const val APKPURE_URL = "https://d.apkpure.com/b/XAPK/$PACKAGE_NAME?version=latest"
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizuku()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        checkShizuku()
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        checkShizuku()
    }

    init {
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        checkShizuku()
        checkRoot()
        refreshVersionInfo()
    }

    override fun onCleared() {
        super.onCleared()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }

    fun checkShizuku() {
        val available = try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
        
        val hasPermission = try {
            available && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }

        val previous = _shizukuAvailable.value
        _shizukuAvailable.value = hasPermission
        
        if (available && !hasPermission) {
            try {
                Shizuku.requestPermission(0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        if (hasPermission && !previous) {
            _useRoot.value = false
        }
    }

    fun checkRoot() {
        viewModelScope.launch(Dispatchers.IO) {
            val available = try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                val exitCode = process.waitFor()
                exitCode == 0
            } catch (_: Exception) {
                false
            }
            val previous = _rootAvailable.value
            _rootAvailable.value = available
            if (available && !previous && !_shizukuAvailable.value) {
                _useRoot.value = true
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
                // Format: "hololive Dreams_VERSION_APKPure.xapk"
                val parts = decoded.removeSuffix(".xapk").removeSuffix(".apk").split("_")
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
            _installLogs.value = listOf("Downloaded file not found!")
            return
        }
        performInstall(downloadFile = downloadFile, uri = null)
    }

    fun installLocalFile(uri: Uri) {
        performInstall(downloadFile = null, uri = uri)
    }

    private fun performInstall(downloadFile: File?, uri: Uri?) {
        _isInstalling.value = true
        _installLogs.value = listOf("Starting installation...")
        
        viewModelScope.launch {
            val isRoot = _useRoot.value
            val success = if (downloadFile != null) {
                Installer.installFile(context, downloadFile, isRoot) { status ->
                    _installLogs.value = _installLogs.value + status
                }
            } else if (uri != null) {
                Installer.installFile(context, uri, isRoot) { status ->
                    _installLogs.value = _installLogs.value + status
                }
            } else {
                false
            }
            
            if (success) {
                _installLogs.value = _installLogs.value + "Installation completed successfully!"
                // Refresh installed version after successful install
                checkInstalledVersion()
            }
            _isInstalling.value = false
        }
    }
}
