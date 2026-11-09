package com.zagio.finanzapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.webkit.WebViewAssetLoader
import java.io.File

class MainActivity : ComponentActivity() {

    companion object {
        // Misma dirección que las versiones anteriores: ahí están guardados los datos.
        private const val APP_URL = "https://appassets.androidplatform.net/assets/index.html"
        private const val CHECK_EVERY_MS = 6 * 60 * 60 * 1000L // comprobar como mucho cada 6 h
        private const val PREFS = "updates"

        // Botón atrás: cierra la hoja abierta o vuelve a la pantalla anterior de la web.
        private const val BACK_JS = "(function(){if(document.querySelector('.scrim')){document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape'}));return 1}" +
            "var b=document.querySelector('.ibtn');if(b){b.click();return 1}" +
            "var t=document.querySelector('.tabs [data-go=inicio]');if(t&&!t.hasAttribute('aria-current')){t.click();return 1}return 0})()"
    }

    private lateinit var webView: WebView
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var updateDialogShown = false
    private var pendingRelease: Updater.Release? = null

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val uris: Array<Uri>? = if (result.resultCode == RESULT_OK && data != null) {
                val clip = data.clipData
                if (clip != null) Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
                else data.data?.let { arrayOf(it) }
            } else null
            fileChooserCallback?.onReceiveValue(uris)
            fileChooserCallback = null
        }

    private val currentVersionCode: Long
        get() = PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0))

    private val currentVersionName: String
        get() = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                @Suppress("DEPRECATION")
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                loadWithOverviewMode = true
                useWideViewPort = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                // Enlaces externos (teléfono, webs...) se abren con la app que corresponda
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (request.url.host == "appassets.androidplatform.net") return false
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    } catch (e: Exception) {
                        toast("No hay ninguna app para abrir este enlace")
                    }
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    if (filePathCallback == null) return false
                    val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = filePathCallback
                    fileChooserLauncher.launch(intent)
                    return true
                }
            }
            addJavascriptInterface(Bridge(), "Android")
        }
        setContentView(webView)
        if (savedInstanceState != null) webView.restoreState(savedInstanceState)
        else webView.loadUrl(APP_URL)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript(BACK_JS) { r ->
                    if (r != "1") finish()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        // Vuelve de los ajustes de "instalar apps desconocidas": continuar con la actualización
        pendingRelease?.let { rel ->
            if (canInstall()) { pendingRelease = null; downloadAndInstall(rel); return }
        }
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (System.currentTimeMillis() - prefs.getLong("lastCheck", 0) > CHECK_EVERY_MS) {
            checkForUpdates(manual = false)
        }
    }

    /** Lo que la web puede pedir a la app: versión instalada y buscar actualizaciones. */
    inner class Bridge {
        @JavascriptInterface
        fun version(): String = currentVersionName

        @JavascriptInterface
        fun checkUpdates() = runOnUiThread { checkForUpdates(manual = true) }
    }

    private fun checkForUpdates(manual: Boolean) {
        Thread {
            val rel = try { Updater.fetchLatest() } catch (e: Exception) { null }
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                if (rel != null) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putLong("lastCheck", System.currentTimeMillis()).apply()
                }
                when {
                    rel != null && rel.code > currentVersionCode -> {
                        if (manual || !updateDialogShown) showUpdateDialog(rel)
                    }
                    manual && rel != null -> toast("Ya tienes la última versión ($currentVersionName)")
                    manual -> toast("No se pudo comprobar. ¿Tienes conexión?")
                }
            }
        }.start()
    }

    private fun showUpdateDialog(rel: Updater.Release) {
        updateDialogShown = true
        val notes = rel.notes.trim().let { if (it.length > 600) it.take(600) + "…" else it }
        AlertDialog.Builder(this)
            .setTitle("Nueva versión: ${rel.name}")
            .setMessage(
                "Tienes la $currentVersionName.\n\n" +
                    (if (notes.isNotEmpty()) "Novedades:\n$notes\n\n" else "") +
                    "Tus datos no se tocan al actualizar."
            )
            .setPositiveButton("Actualizar") { _, _ -> startUpdate(rel) }
            .setNegativeButton("Más tarde", null)
            .show()
    }

    private fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()

    private fun startUpdate(rel: Updater.Release) {
        if (canInstall()) { downloadAndInstall(rel); return }
        AlertDialog.Builder(this)
            .setTitle("Permitir actualizaciones")
            .setMessage("Para instalar la nueva versión, Android necesita que permitas a Finanzapp instalar aplicaciones. Activa el permiso y vuelve atrás.")
            .setPositiveButton("Abrir ajustes") { _, _ ->
                pendingRelease = rel
                startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun downloadAndInstall(rel: Updater.Release) {
        val pad = (20 * resources.displayMetrics.density).toInt()
        val label = TextView(this).apply { text = "Descargando…" }
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; isIndeterminate = true
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad / 2)
            addView(label); addView(bar)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Actualizando Finanzapp")
            .setView(box)
            .setCancelable(false)
            .show()

        val apk = File(File(cacheDir, "updates"), "finanzapp.apk")
        Thread {
            try {
                Updater.download(rel.apkUrl, apk) { p ->
                    runOnUiThread {
                        if (p >= 0) { bar.isIndeterminate = false; bar.progress = p; label.text = "Descargando… $p %" }
                    }
                }
                runOnUiThread { dialog.dismiss(); install(apk) }
            } catch (e: Exception) {
                runOnUiThread { dialog.dismiss(); toast("No se pudo descargar la actualización") }
            }
        }.start()
    }

    private fun install(apk: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
