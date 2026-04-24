package uz.kidzone.app;

import android.annotation.SuppressLint;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class KidWebViewManager {
    private final WebView webView;

    public KidWebViewManager(WebView webView) {
        this.webView = webView;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void setup(Object jsInterface, String interfaceName) {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // Performance optimization
        s.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        webView.addJavascriptInterface(jsInterface, interfaceName);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return !req.getUrl().toString().startsWith("file://");
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
    }

    public void loadUrl(String url) {
        webView.loadUrl(url);
    }

    public boolean canGoBack() {
        return webView.canGoBack();
    }

    public void goBack() {
        webView.goBack();
    }

    public void destroy() {
        if (webView != null) {
            webView.destroy();
        }
    }

    public void evaluateJavascript(String script) {
        webView.evaluateJavascript(script, null);
    }
}
