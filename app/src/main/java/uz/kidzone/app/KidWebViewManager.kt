package uz.kidzone.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Manages WebView configuration and operations.
 * Decouples WebView logic from the Activity.
 */
class KidWebViewManager(private val webView: WebView) {

    companion object {
        private const val TAG = "KidWebViewManager"
    }

    var currentLanguage: String = "en"

    fun setLanguage(lang: String) {
        currentLanguage = lang
    }

    fun getLanguage(): String = currentLanguage

    // JavascriptInterface: lint can't see through the generic `Any` parameter to confirm
    // the passed object's methods carry @JavascriptInterface — they do (NativeBridge,
    // ChallengeBridge in MainScreen.kt), this is a lint false positive on the wrapper shape.
    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    fun setup(jsInterface: Any, interfaceName: String) {
        // Enable remote debugging via chrome://inspect
        WebView.setWebContentsDebuggingEnabled(true)

        applySettings(webView.settings)

        webView.addJavascriptInterface(jsInterface, interfaceName)
        webView.webViewClient = InternalWebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                Log.d("KZ-JS", "${cm.messageLevel()} [${cm.sourceId()}:${cm.lineNumber()}] ${cm.message()}")
                return true
            }
        }
    }

    @Suppress("SetJavaScriptEnabled")
    private fun applySettings(settings: WebSettings) {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        // Allow JS inside file:// pages to fetch/XHR other local assets
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = true
        @Suppress("DEPRECATION")
        settings.allowUniversalAccessFromFileURLs = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
    }

    @SuppressLint("JavascriptInterface")
    fun addInterface(obj: Any, name: String) {
        webView.addJavascriptInterface(obj, name)
    }

    fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    fun canGoBack(): Boolean = webView.canGoBack()

    fun goBack() {
        webView.goBack()
    }

    fun evaluateJavascript(script: String) {
        webView.post { webView.evaluateJavascript(script, null) }
    }

    fun evaluateJavascript(script: String, cb: ValueCallback<String>) {
        webView.post { webView.evaluateJavascript(script, cb) }
    }

    fun destroy() {
        webView.stopLoading()
        webView.onPause()
        webView.removeAllViews()
        webView.destroy()
    }

    private inner class InternalWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            if (url.startsWith("file://")) return false
            try {
                view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Log.w(TAG, "Cannot open external URL: $url")
            }
            return true
        }
    }
}
