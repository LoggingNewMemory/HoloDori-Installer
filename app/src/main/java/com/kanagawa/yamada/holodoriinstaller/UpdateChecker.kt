package com.kanagawa.yamada.holodoriinstaller

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URLDecoder

object UpdateChecker {
    private const val APKPURE_URL = "https://d.apkpure.com/b/XAPK/game.qualiarts.hololive.dreams.com?version=latest"

    suspend fun fetchLatestVersionFromApkPure(): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> {
                        val addresses = Dns.SYSTEM.lookup(hostname)
                        val ipv4 = addresses.filterIsInstance<Inet4Address>()
                        return ipv4.ifEmpty { addresses }
                    }
                })
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

    fun getInstalledVersion(context: Context): String? {
        return try {
            val pInfo = context.packageManager.getPackageInfo("game.qualiarts.hololive.dreams.com", 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
