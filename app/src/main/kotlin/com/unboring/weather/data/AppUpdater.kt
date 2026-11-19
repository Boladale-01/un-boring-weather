package com.unboring.weather.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AppReleaseInfo(
  val tagName: String,
  val notes: String,
  val apkDownloadUrl: String,
  val htmlUrl: String
)

/**
 * Checks GitHub Releases for newer APK versions and installs them in-app.
 * Update [GITHUB_OWNER] and [GITHUB_REPO] to match your public GitHub repository.
 */
class AppUpdater(private val context: Context) {

  companion object {
    const val GITHUB_OWNER = "Boladale-01"
    const val GITHUB_REPO = "un-boring-weather"
    const val CURRENT_VERSION_NAME = "1.0"
  }

  private val json = Json { ignoreUnknownKeys = true }

  suspend fun checkForUpdate(): AppReleaseInfo? = withContext(Dispatchers.IO) {
    if (GITHUB_OWNER == "YOUR_GITHUB_USERNAME") return@withContext null
    try {
      val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
      val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 6000
        readTimeout = 6000
        setRequestProperty("Accept", "application/vnd.github+json")
      }
      if (conn.responseCode !in 200..299) return@withContext null
      val body = conn.inputStream.bufferedReader().use { it.readText() }
      val root = json.parseToJsonElement(body).jsonObject
      val tag = root["tag_name"]?.jsonPrimitive?.content ?: return@withContext null
      val cleanRemote = tag.removePrefix("v").trim()
      if (!isNewerVersion(cleanRemote, CURRENT_VERSION_NAME)) return@withContext null

      val htmlUrl = root["html_url"]?.jsonPrimitive?.content ?: "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
      val notes = root["body"]?.jsonPrimitive?.content ?: ""
      val assets = root["assets"]?.jsonArray
      val apkUrl = assets?.firstOrNull {
        it.jsonObject["name"]?.jsonPrimitive?.content?.endsWith(".apk", ignoreCase = true) == true
      }?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.content ?: htmlUrl

      AppReleaseInfo(tagName = tag, notes = notes, apkDownloadUrl = apkUrl, htmlUrl = htmlUrl)
    } catch (_: Exception) {
      null
    }
  }

  suspend fun downloadAndInstall(apkUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      if (!apkUrl.endsWith(".apk", ignoreCase = true)) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
        return@withContext Result.success(Unit)
      }

      val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
      val outFile = File(dir, "un-boring-weather-update.apk")
      val conn = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = true
        connectTimeout = 15000
        readTimeout = 30000
      }
      conn.inputStream.use { input ->
        FileOutputStream(outFile).use { output ->
          input.copyTo(output)
        }
      }

      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outFile
      )
      val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(installIntent)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun isNewerVersion(remote: String, current: String): Boolean {
    val rParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
    val cParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    val maxLen = maxOf(rParts.size, cParts.size)
    for (i in 0 until maxLen) {
      val r = rParts.getOrElse(i) { 0 }
      val c = cParts.getOrElse(i) { 0 }
      if (r != c) return r > c
    }
    return false
  }
}
