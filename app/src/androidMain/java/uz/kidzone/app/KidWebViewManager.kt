package uz.kidzone.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import uz.kidzone.app.BuildConfig

/**
 * Manages WebView configuration and operations.
 * Decouples WebView logic from the Activity.
 */
class KidWebViewManager(private val webView: WebView) {

    companion object {
        private const val TAG = "KidWebViewManager"
        const val ASSET_BASE_URL = "https://appassets.androidplatform.net/assets/www/"
    }

    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(webView.context))
        .build()

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
        // Remote debugging via chrome://inspect — debug builds only. Leaving this on
        // in release lets anyone with ADB/USB access to the device inspect and
        // manipulate the WebView contents (e.g. bypass the parental PIN gate JS).
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        applySettings(webView.settings)

        webView.addJavascriptInterface(jsInterface, interfaceName)
        webView.webViewClient = InternalWebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                Log.d("KZ-JS", "${cm.messageLevel()} [${cm.sourceId()}:${cm.lineNumber()}] ${cm.message()}")
                return true
            }
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                result?.confirm()
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
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
    }

    @SuppressLint("JavascriptInterface")
    fun addInterface(obj: Any, name: String) {
        webView.addJavascriptInterface(obj, name)
    }

    fun loadGame(gameId: String) {
        val cleanId = if (gameId.endsWith(".html")) gameId else "$gameId.html"
        val url = "$ASSET_BASE_URL$cleanId"
        webView.loadUrl(url)
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
        webView.clearHistory()
        webView.clearCache(true)
        webView.removeAllViews()
        webView.destroy()
    }

    private inner class InternalWebViewClient : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val intercepted = assetLoader.shouldInterceptRequest(request.url)
            return intercepted ?: super.shouldInterceptRequest(view, request)
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            if (url.startsWith("https://appassets.androidplatform.net") ||
                url.startsWith("file://") ||
                url.startsWith("http://localhost")) {
                return false
            }
            try {
                view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Log.w(TAG, "Cannot open external URL: $url")
            }
            return true
        }
    }
}
