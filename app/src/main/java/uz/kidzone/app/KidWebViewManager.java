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
    private Runnable onPageReadyCallback;
    private boolean pageReadyCalled = false;

    public KidWebViewManager(WebView webView) {
        this.webView = webView;
    }

    public void setLanguage(String lang) {
        this.currentLanguage = lang;
    }

    public String getLanguage() {
        return currentLanguage;
    }

    public void setOnPageReadyCallback(Runnable callback) {
        this.onPageReadyCallback = callback;
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

    public void evaluateJavascript(String script, android.webkit.ValueCallback<String> cb) {
        if (webView != null) {
            webView.post(() -> webView.evaluateJavascript(script, cb));
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

    private class InternalWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (url.startsWith("file://")) return false;
            try {
                android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url)
                );
                view.getContext().startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "Cannot open external URL: " + url);
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!pageReadyCalled && onPageReadyCallback != null) {
                pageReadyCalled = true;
                view.post(onPageReadyCallback);
            }
        }
    }
}
