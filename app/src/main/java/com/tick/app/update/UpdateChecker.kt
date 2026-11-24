package com.tick.app.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val notes: String,
)

object UpdateChecker {
    private const val CONNECT_TIMEOUT = 12_000
    private const val READ_TIMEOUT = 20_000

    fun currentVersionCode(context: Context): Int =
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        }.getOrDefault(0)

    fun currentVersionName(context: Context): String =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("")

    suspend fun check(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            if (!UpdateConfig.isConfigured) return@runCatching null
            val connection = (URL(UpdateConfig.updateJsonUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Tick-Android")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    error("HTTP ${connection.responseCode}")
                }
                val raw = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(raw)
                UpdateInfo(
                    versionCode = json.optInt("versionCode", 0),
                    versionName = json.optString("versionName"),
                    apkUrl = json.optString("apkUrl"),
                    sha256 = json.optString("sha256"),
                    notes = json.optString("notes"),
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(info.apkUrl.startsWith("https://")) { "更新地址必须是 HTTPS" }
            val targetDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = File(targetDir, "tick-${info.versionCode}.apk")
            val connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Tick-Android")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    error("HTTP ${connection.responseCode}")
                }
                val total = connection.contentLengthLong.takeIf { it > 0L } ?: -1L
                var copied = 0L
                connection.inputStream.use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            if (total > 0) onProgress((copied.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
            if (info.sha256.isNotBlank() && !sha256(target).equals(info.sha256, ignoreCase = true)) {
                target.delete()
                error("安装包校验失败")
            }
            verifyPackage(context, target, info.versionCode)
            onProgress(1f)
            target
        }
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun verifyPackage(context: Context, apk: File, expectedVersionCode: Int) {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: error("无法读取安装包")
        require(archive.packageName == context.packageName) { "安装包包名不匹配" }
        @Suppress("DEPRECATION")
        require(archive.versionCode >= expectedVersionCode) { "安装包版本不正确" }
        val installedSignatures = signingHashes(context, null) ?: error("无法读取当前签名")
        val archiveSignatures = signingHashes(context, apk.absolutePath) ?: error("无法读取安装包签名")
        require(installedSignatures == archiveSignatures) { "安装包签名与当前应用不一致" }
    }

    private fun signingHashes(context: Context, apkPath: String?): Set<String>? {
        val pm = context.packageManager
        val info = runCatching {
            if (apkPath == null) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNING_CERTIFICATES)
            }
        }.getOrNull() ?: return null
        val signingInfo = info.signingInfo ?: return null
        val signers = signingInfo.apkContentsSigners ?: return null
        return signers.map { sha256(it.toByteArray()) }.toSet()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
