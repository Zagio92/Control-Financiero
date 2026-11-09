package com.zagio.finanzapp

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Consulta la última Release de GitHub y descarga su APK. */
object Updater {
    private const val REPO = "Zagio92/Control-Financiero"
    private const val API = "https://api.github.com/repos/$REPO/releases/latest"

    data class Release(val code: Int, val name: String, val notes: String, val apkUrl: String)

    /** Devuelve la última versión publicada, o null si no hay o no se pudo consultar. */
    fun fetchLatest(): Release? {
        val con = (URL(API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "FinanzApp-Android")
        }
        try {
            if (con.responseCode != 200) return null
            val json = JSONObject(con.inputStream.bufferedReader().use { it.readText() })
            // Las etiquetas son "v<versionCode>", por ejemplo v12
            val code = json.optString("tag_name").removePrefix("v").toIntOrNull() ?: return null
            val assets = json.optJSONArray("assets") ?: return null
            var apk: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk")) {
                    apk = a.optString("browser_download_url"); break
                }
            }
            if (apk.isNullOrEmpty()) return null
            return Release(
                code = code,
                name = json.optString("name").ifEmpty { "Versión $code" },
                notes = json.optString("body"),
                apkUrl = apk
            )
        } finally {
            con.disconnect()
        }
    }

    /** Descarga el APK en [dest], avisando del progreso (0-100, o -1 si no se sabe el tamaño). */
    fun download(url: String, dest: File, onProgress: (Int) -> Unit) {
        dest.parentFile?.mkdirs()
        val con = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "FinanzApp-Android")
        }
        try {
            if (con.responseCode !in 200..299) error("HTTP ${con.responseCode}")
            val total = con.contentLengthLong
            var done = 0L
            var last = -2
            con.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        val p = if (total > 0) (done * 100 / total).toInt() else -1
                        if (p != last) { last = p; onProgress(p) }
                    }
                }
            }
        } finally {
            con.disconnect()
        }
    }
}
