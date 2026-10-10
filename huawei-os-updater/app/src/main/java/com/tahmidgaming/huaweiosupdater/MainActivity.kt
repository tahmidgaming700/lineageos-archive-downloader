package com.tahmidgaming.huaweiosupdater

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var status: TextView

    private val homeUrl = "https://professorjtj.github.io/"
    private val allowedHosts = setOf("professorjtj.github.io")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF4F6FA.toInt())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(12))
            setBackgroundColor(0xFF101820.toInt())
        }

        val title = TextView(this).apply {
            text = "Huawei OS Updater"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER_VERTICAL
        }
        val subtitle = TextView(this).apply {
            text = "Firmware finder • archives • downloads"
            textSize = 13f
            setTextColor(0xFFCAD5E2.toInt())
            setPadding(0, dp(3), 0, dp(10))
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        actions.addView(actionButton("Finder") { load(homeUrl) })
        actions.addView(actionButton("v2") { load("https://professorjtj.github.io/v2/") })
        actions.addView(actionButton("Base archive") { load("https://professorjtj.github.io/BaseArchive/") })
        actions.addView(actionButton("Downgrades") { load("https://professorjtj.github.io/Downgrade/") })

        header.addView(title)
        header.addView(subtitle)
        header.addView(actions)

        status = TextView(this).apply {
            text = "Connecting to Huawei Firm Finder…"
            textSize = 12f
            setTextColor(0xFF344054.toInt())
            setPadding(dp(12), dp(7), dp(12), dp(7))
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            CookieManager.getInstance().setAcceptCookie(true)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (uri.scheme == "https" && uri.host in allowedHosts) return false
                    if (uri.scheme == "https" || uri.scheme == "http") {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                        return true
                    }
                    return true
                }

                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    status.text = "Loading firmware listings…"
                }

                override fun onPageFinished(view: WebView, url: String) {
                    status.text = "Source: professorjtj.github.io"
                }
            }
            setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                try {
                    val uri = Uri.parse(url)
                    if (uri.scheme != "https") {
                        Toast.makeText(this@MainActivity, "Only HTTPS downloads are allowed.", Toast.LENGTH_LONG).show()
                        return@DownloadListener
                    }
                    val request = DownloadManager.Request(uri)
                    request.setMimeType(mimeType)
                    request.addRequestHeader("User-Agent", userAgent)
                    val cookies = CookieManager.getInstance().getCookie(url)
                    if (!cookies.isNullOrBlank()) request.addRequestHeader("Cookie", cookies)
                    request.setTitle("Huawei firmware download")
                    request.setDescription(contentDisposition ?: "Downloading firmware file")
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileNameFrom(url, contentDisposition))
                    val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    manager.enqueue(request)
                    Toast.makeText(this@MainActivity, "Download added to Android Downloads.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Could not start download: ${e.localizedMessage ?: "unknown error"}", Toast.LENGTH_LONG).show()
                }
            })
        }

        root.addView(header)
        root.addView(status)
        root.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        if (savedInstanceState == null) load(homeUrl) else webView.restoreState(savedInstanceState)
    }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            setPadding(dp(3), 0, dp(3), 0)
            setOnClickListener { action() }
        }

    private fun load(url: String) {
        webView.loadUrl(url)
    }

    private fun fileNameFrom(url: String, disposition: String?): String {
        val fromDisposition = disposition?.substringAfter("filename=", "")?.trim('"', '\'')
        val candidate = fromDisposition?.takeIf { it.isNotBlank() }
            ?: Uri.parse(url).lastPathSegment
            ?: "huawei-firmware.bin"
        return candidate.replace(Regex("[^A-Za-z0-9._-]"), "_").take(180)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
