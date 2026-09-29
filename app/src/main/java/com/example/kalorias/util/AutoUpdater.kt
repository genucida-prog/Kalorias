package com.example.kalorias.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object AutoUpdater {
    private const val GITHUB_REPO_API = "https://api.github.com/repos/genucida-prog/Kalorias/releases/latest"

    suspend fun checkForUpdates(context: Context, currentVersion: String, onUpdateAvailable: (String, String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val response = URL(GITHUB_REPO_API).readText()
                val json = JSONObject(response)
                val latestTag = json.getString("tag_name") // e.g. "v1.1"
                val assets = json.optJSONArray("assets")
                val apkUrl = if (assets != null && assets.length() > 0) {
                    assets.getJSONObject(0).getString("browser_download_url")
                } else {
                    json.getString("html_url")
                }

                if (latestTag != currentVersion && latestTag.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        onUpdateAvailable(latestTag, apkUrl)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun downloadAndInstallApk(context: Context, apkUrl: String, versionName: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
                setTitle("Actualizando Kalorias $versionName")
                setDescription("Descargando nueva versión desde GitHub...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Kalorias-$versionName.apk")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(context, "Descargando actualización en Descargas...", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error al iniciar descarga: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
