package com.kanagawa.yamada.holodoriinstaller

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

enum class DownloadState {
    IDLE, DOWNLOADING, PAUSED, FINISHED, ERROR, CANCELLED
}

class Downloader(private val targetFile: File) {

    private val _downloadState = MutableStateFlow(DownloadState.IDLE)
    val downloadState: StateFlow<DownloadState> = _downloadState

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _downloadedBytes = MutableStateFlow(0L)
    val downloadedBytes: StateFlow<Long> = _downloadedBytes

    private val _totalBytes = MutableStateFlow(0L)
    val totalBytes: StateFlow<Long> = _totalBytes

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private var downloadJob: Job? = null
    
    // Use a custom user agent to bypass some blocks
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun startDownload(url: String) {
        if (_downloadState.value == DownloadState.DOWNLOADING) return
        
        _downloadState.value = DownloadState.DOWNLOADING
        _errorMessage.value = ""

        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val downloadedLength = if (targetFile.exists()) targetFile.length() else 0L
                _downloadedBytes.value = downloadedLength

                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                
                if (downloadedLength > 0) {
                    requestBuilder.header("Range", "bytes=$downloadedLength-")
                }

                val response = client.newCall(requestBuilder.build()).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }

                val body = response.body ?: throw IOException("Empty body")
                
                // If the server doesn't support partial content, we might get the full file again
                val isPartial = response.code == 206
                val append = isPartial && downloadedLength > 0
                
                val contentLength = body.contentLength()
                if (contentLength > 0) {
                    _totalBytes.value = if (isPartial) downloadedLength + contentLength else contentLength
                } else if (_totalBytes.value == 0L) {
                    // Try to extract from Content-Range if available
                    val contentRange = response.header("Content-Range")
                    if (contentRange != null) {
                        val total = contentRange.substringAfterLast("/").toLongOrNull()
                        if (total != null) _totalBytes.value = total
                    }
                }

                if (!append && downloadedLength > 0) {
                    targetFile.delete()
                    _downloadedBytes.value = 0L
                }

                body.byteStream().use { input ->
                    FileOutputStream(targetFile, append).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (!isActive) break // Coroutine cancelled
                            
                            output.write(buffer, 0, bytesRead)
                            _downloadedBytes.value += bytesRead
                            if (_totalBytes.value > 0) {
                                _progress.value = _downloadedBytes.value.toFloat() / _totalBytes.value.toFloat()
                            }
                        }
                    }
                }
                
                if (isActive) {
                    // If we finished downloading and wasn't cancelled
                    if (_totalBytes.value <= 0 || _downloadedBytes.value >= _totalBytes.value) {
                        _progress.value = 1f
                        _downloadState.value = DownloadState.FINISHED
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _errorMessage.value = e.message ?: "Unknown error"
                    _downloadState.value = DownloadState.ERROR
                }
            }
        }
    }

    fun pause() {
        if (_downloadState.value == DownloadState.DOWNLOADING) {
            downloadJob?.cancel()
            _downloadState.value = DownloadState.PAUSED
        }
    }

    fun cancel() {
        downloadJob?.cancel()
        if (targetFile.exists()) {
            targetFile.delete()
        }
        _downloadedBytes.value = 0L
        _progress.value = 0f
        _downloadState.value = DownloadState.CANCELLED
    }
}
