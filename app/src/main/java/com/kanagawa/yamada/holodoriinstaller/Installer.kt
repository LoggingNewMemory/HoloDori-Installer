package com.kanagawa.yamada.holodoriinstaller

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipFile

object Installer {

    suspend fun installFile(context: Context, uri: Uri, isRoot: Boolean, onProgress: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress("Copying file to cache...")
            val file = File(context.cacheDir, "temp_install_file")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return@withContext installFile(context, file, isRoot, onProgress)
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress("Error: ${e.message}")
            return@withContext false
        }
    }

    suspend fun installFile(context: Context, file: File, isRoot: Boolean, onProgress: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        if (file.name.endsWith(".xapk") || isXapk(file)) {
            onProgress("Extracting XAPK...")
            val extractDir = File(context.cacheDir, "xapk_extract")
            extractDir.deleteRecursively()
            extractDir.mkdirs()
            
            try {
                ZipFile(file).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        if (!entry.isDirectory && entry.name.endsWith(".apk")) {
                            val outFile = File(extractDir, entry.name.substringAfterLast("/"))
                            zip.getInputStream(entry).use { input ->
                                outFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
                onProgress("Installing split APKs...")
                return@withContext installSplitApks(extractDir.listFiles()?.toList() ?: emptyList(), isRoot, onProgress)
            } catch (e: Exception) {
                e.printStackTrace()
                onProgress("Extraction failed: ${e.message}")
                return@withContext false
            }
        } else {
            onProgress("Installing APK...")
            return@withContext installSingleApk(file, isRoot, onProgress)
        }
    }

    private fun isXapk(file: File): Boolean {
        return try {
            ZipFile(file).use { zip ->
                zip.getEntry("manifest.json") != null
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun installSingleApk(file: File, isRoot: Boolean, onProgress: (String) -> Unit): Boolean {
        val cmd = "pm install -i com.android.vending -r \"${file.absolutePath}\""
        return runCommand(cmd, isRoot, onProgress)
    }

    private suspend fun installSplitApks(apks: List<File>, isRoot: Boolean, onProgress: (String) -> Unit): Boolean {
        if (apks.isEmpty()) {
            onProgress("No APKs found to install.")
            return false
        }
        
        val totalSize = apks.sumOf { it.length() }
        val createCmd = "pm install-create -i com.android.vending -S $totalSize"
        val createOutput = runCommandAndGetOutput(createCmd, isRoot)
        
        val sessionIdMatch = Regex("""\[(\d+)]""").find(createOutput)
        val sessionId = sessionIdMatch?.groupValues?.get(1)
        if (sessionId == null) {
            onProgress("Failed to create install session: $createOutput")
            return false
        }
        
        for ((index, apk) in apks.withIndex()) {
            onProgress("Writing split ${index + 1}/${apks.size}...")
            val writeCmd = "pm install-write -S ${apk.length()} $sessionId \"split_$index\" \"${apk.absolutePath}\""
            val writeOutput = runCommandAndGetOutput(writeCmd, isRoot)
            if (!writeOutput.contains("Success", ignoreCase = true)) {
                onProgress("Failed to write split $index: $writeOutput")
                runCommandAndGetOutput("pm install-abandon $sessionId", isRoot)
                return false
            }
        }
        
        onProgress("Committing installation...")
        val commitCmd = "pm install-commit $sessionId"
        val commitOutput = runCommandAndGetOutput(commitCmd, isRoot)
        val success = commitOutput.contains("Success", ignoreCase = true)
        if (success) {
            onProgress("Installation complete!")
        } else {
            onProgress("Failed to commit: $commitOutput")
        }
        return success
    }

    private fun runCommandAndGetOutput(cmd: String, isRoot: Boolean): String {
        return try {
            val process = if (isRoot) {
                Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            } else {
                val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                method.isAccessible = true
                method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            }
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            output + error
        } catch (e: Exception) {
            e.printStackTrace()
            e.message ?: "Unknown error"
        }
    }

    private fun runCommand(cmd: String, isRoot: Boolean, onProgress: (String) -> Unit): Boolean {
        val output = runCommandAndGetOutput(cmd, isRoot)
        val success = output.contains("Success", ignoreCase = true)
        if (success) {
            onProgress("Installation complete!")
        } else {
            onProgress("Installation failed: $output")
        }
        return success
    }
}
