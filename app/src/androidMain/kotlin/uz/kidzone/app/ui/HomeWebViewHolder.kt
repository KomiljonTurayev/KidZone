package uz.kidzone.app.ui

import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import uz.kidzone.app.KidWebViewManager

/**
 * Keeps the home WebView alive while the child is on another screen (sleep, dashboard,
 * sign-in, paywall). Leaving "main" used to destroy it, so every return re-parsed index.html
 * and re-ran all its scripts — the slowest thing the app did. Now the page is paused and
 * re-attached; it is destroyed only when the whole app UI goes away ([destroy]).
 *
 * The JS bridge outlives a MainScreen composition, so it must not capture that composition's
 * state: it calls through [callbacks], which every MainScreen composition replaces.
 */
class HomeWebViewHolder {

    /** What a page was loaded with; a change while the child was away means a reload. */
    data class LoadKey(val lang: String, val age: String, val profile: String, val restricted: Boolean)

    /** Everything the bridge does that touches MainScreen's state. */
    class Callbacks(
        val openKidzo: () -> Unit = {},
        val askKidzo: (String) -> Unit = {},
        val isAiAvailable: () -> Boolean = { false },
        val onAiLocked: () -> Unit = {},
        val onAiDailyLimit: () -> Unit = {},
        val onLevelReached: (String, Int) -> Unit = { _, _ -> },
        val onThemeModeChanged: (String) -> Unit = {},
        val onLanguageChanged: (String) -> Unit = {},
        val onAgeChanged: (String) -> Unit = {},
        val onOpenDashboard: () -> Unit = {},
        val onOpenSleep: () -> Unit = {},
        val onOpenPrivacy: () -> Unit = {},
        val onExternalLink: (Uri) -> Unit = {},
    )

    @Volatile var callbacks = Callbacks()

    var webView: WebView? = null
        private set
    var manager: KidWebViewManager? = null
        private set
    private var bridge: NativeBridge? = null
    var loadedKey: LoadKey? = null

    internal fun attach(webView: WebView, manager: KidWebViewManager, bridge: NativeBridge, key: LoadKey) {
        this.webView = webView
        this.manager = manager
        this.bridge = bridge
        this.loadedKey = key
    }

    /**
     * Returns the kept WebView ready to be placed in a new AndroidView, reloading it first if
     * the child's language, age, profile or restriction changed meanwhile.
     */
    fun reuse(key: LoadKey, url: String): WebView? {
        val view = webView ?: return null
        (view.parent as? ViewGroup)?.removeView(view)
        view.onResume()
        if (key != loadedKey) {
            loadedKey = key
            view.loadUrl(url)
        }
        return view
    }

    /** The child left the home screen: silence it and stop its timers' work. */
    fun pause() {
        val view = webView ?: return
        bridge?.stopSpeaking()
        // onPause() does not promise to stop HTML media; the games run in a same-origin iframe.
        view.evaluateJavascript(PAUSE_MEDIA_JS, null)
        view.onPause()
    }

    fun destroy() {
        manager?.destroy()
        bridge?.shutdownTts()
        webView = null
        manager = null
        bridge = null
        loadedKey = null
    }

    private companion object {
        const val PAUSE_MEDIA_JS = """(function(){
            function stop(d){try{d.querySelectorAll('audio,video').forEach(function(m){m.pause()});
              d.querySelectorAll('iframe').forEach(function(f){try{stop(f.contentDocument)}catch(e){}})}catch(e){}}
            stop(document);
            try{window.speechSynthesis&&speechSynthesis.cancel()}catch(e){}
        })()"""
    }
}
