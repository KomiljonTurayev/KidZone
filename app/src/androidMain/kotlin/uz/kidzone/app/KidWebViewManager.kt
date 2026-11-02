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

    /**
     * Called for every navigation that leaves the bundled assets. Google Play's Families policy
     * requires a parental gate before a kids app sends the child out of the app, so the host
     * shows one and calls [openExternal] only after a grown-up passes it. If unset, external
     * links are blocked.
     */
    var onExternalLink: ((Uri) -> Unit)? = null

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
        // Games are served exclusively via WebViewAssetLoader over the synthetic
        // https://appassets.androidplatform.net origin (see ASSET_BASE_URL) — the WebView
        // never navigates to a file:// URL. file access (and its cross-file-origin variants)
        // stay off so a bug in any bundled game's JS can't be leveraged to read arbitrary
        // local files; this is Google's documented WebView hardening baseline.
        settings.allowFileAccess = false
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

    /** Opens [uri] outside the app. Call only after the parental gate has been passed. */
    fun openExternal(uri: Uri) {
        try {
            webView.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Log.w(TAG, "Cannot open external URL: $uri")
        }
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
            if (request.url.scheme == "https" && request.url.host == "appassets.androidplatform.net") {
                return false
            }
            // Only a tap may start the gate; a page redirecting on its own is just blocked.
            if (request.hasGesture()) {
                onExternalLink?.invoke(request.url)
            } else {
                Log.w(TAG, "Blocked external navigation without user gesture: ${request.url}")
            }
            return true
        }
    }
}
