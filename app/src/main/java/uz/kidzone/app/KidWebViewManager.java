package uz.kidzone.app;

import android.annotation.SuppressLint;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Manages WebView configuration and operations.
 * Decouples WebView logic from the Activity.
 */
public class KidWebViewManager {
    private static final String TAG = "KidWebViewManager";
    private final WebView webView;
    private String currentLanguage = "en";

    public KidWebViewManager(WebView webView) {
        this.webView = webView;
    }

    public void setLanguage(String lang) {
        this.currentLanguage = lang;
    }

    public String getLanguage() {
        return currentLanguage;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void setup(Object jsInterface, String interfaceName) {
        if (webView == null) {
            Log.e(TAG, "WebView is null, cannot setup");
            return;
        }

        // Enable remote debugging via chrome://inspect
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        WebSettings settings = webView.getSettings();
        applySettings(settings);

        webView.addJavascriptInterface(jsInterface, interfaceName);
        webView.setWebViewClient(new InternalWebViewClient());
        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage cm) {
                Log.d("KZ-JS", cm.messageLevel() + " [" + cm.sourceId() + ":" + cm.lineNumber() + "] " + cm.message());
                return true;
            }
        });
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private void applySettings(WebSettings settings) {
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        // Allow JS inside file:// pages to fetch/XHR other local assets
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
    }

    public void loadUrl(String url) {
        if (webView != null) {
            webView.loadUrl(url);
        }
    }

    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        if (webView != null) {
            webView.goBack();
        }
    }

    public void evaluateJavascript(String script) {
        if (webView != null) {
            webView.post(() -> webView.evaluateJavascript(script, null));
        }
    }

    public void destroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.onPause();
            webView.removeAllViews();
            webView.destroy();
        }
    }

    private static class InternalWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            // Only allow local file navigation for security
            return !url.startsWith("file://");
        }
    }
}
