package com.epikaigle.lemondashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkSize: Long,
    val publishedAt: String,
    val isUpdateAvailable: Boolean
)

object AppUpdateManager {
    private const val GITHUB_REPO = "Epikaigle/lemonsqueezy-android-dashboard"
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Compares two semantic version strings (e.g., "1.5.2" vs "1.5.3" or "v1.5.2").
     * Returns true if [latest] is strictly newer than [current].
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        fun parse(v: String): List<Int> {
            val cleaned = v.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore("-") // strip prerelease tags like -beta
            return cleaned.split(".")
                .mapNotNull { it.toIntOrNull() }
        }

        val curParts = parse(current)
        val latParts = parse(latest)
        val maxLen = maxOf(curParts.size, latParts.size)

        for (i in 0 until maxLen) {
            val c = curParts.getOrElse(i) { 0 }
            val l = latParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    /**
     * Checks GitHub for the latest release and returns update metadata.
     */
    suspend fun checkForUpdate(currentVersion: String = BuildConfig.VERSION_NAME): AppUpdateInfo? =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "LemonDashboard-Android/${currentVersion}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                throw IOException("GitHub API a répondu avec le code $code")
            }

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return@withContext null

            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "").trim()
            val latestVersion = tagName.removePrefix("v").removePrefix("V")
            val releaseTitle = json.optString("name", "Version $tagName")
            val releaseNotes = json.optString("body", "").trim()
            val publishedAt = json.optString("published_at", "")

            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            var apkSize = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        if (name.equals("app-release.apk", ignoreCase = true)) {
                            // Perfect match
                            break
                        }
                    }
                }
            }

            if (apkUrl.isBlank()) {
                throw IOException("Aucun fichier APK trouvé dans la dernière version ($tagName)")
            }

            val isAvailable = isNewerVersion(currentVersion, latestVersion)

            AppUpdateInfo(
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                apkUrl = apkUrl,
                apkSize = apkSize,
                publishedAt = publishedAt,
                isUpdateAvailable = isAvailable
            )
        }

    /**
     * Downloads the APK file to cache and reports progress.
     */
    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        targetVersion: String,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }

        // Clean any older downloaded apk files
        updatesDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk") || file.name.endsWith(".tmp")) {
                runCatching { file.delete() }
            }
        }

        val finalFile = File(updatesDir, "lemon-dashboard-v${targetVersion}.apk")
        val tempFile = File(updatesDir, "lemon-dashboard-v${targetVersion}.apk.tmp")

        val request = Request.Builder()
            .url(apkUrl)
            .header("User-Agent", "LemonDashboard-Android")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            throw IOException("Échec du téléchargement (code HTTP $code)")
        }

        val responseBody = response.body ?: throw IOException("Réponse de téléchargement vide")
        val totalBytes = responseBody.contentLength()

        responseBody.byteStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                var lastReported = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    // Throttle updates slightly to avoid overwhelming Compose state
                    if (totalRead - lastReported >= 65536 || totalRead == totalBytes) {
                        lastReported = totalRead
                        onProgress(totalRead, totalBytes)
                    }
                }
                output.flush()
            }
        }

        if (tempFile.exists()) {
            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }
        }

        if (!finalFile.exists() || finalFile.length() == 0L) {
            throw IOException("Fichier APK téléchargé invalide ou introuvable")
        }

        finalFile
    }

    /**
     * Checks if the app is allowed to install packages (Android 8.0+).
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Creates intent to open system settings where user can toggle "Install unknown apps".
     */
    fun createInstallPermissionIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Triggers package installer with the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Formats bytes into human-readable string (e.g. 3,4 Mo).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 o"
        val units = arrayOf("o", "Ko", "Mo", "Go")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.FRENCH, "%.1f %s", value, units[digitGroups])
    }
}
