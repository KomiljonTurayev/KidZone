package uz.kidzone.app;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class MainActivity extends AppCompatActivity {

    private static final int INTERSTITIAL_EVERY = 3;

    private WebView webView;
    private AdsManager adsManager;
    private int gameCount = 0;
    private boolean isTablet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        setupWindow();
        setContentView(R.layout.activity_main);

        isTablet = checkIsTablet();
        adsManager = new AdsManager(this);
        adsManager.loadBanner(findViewById(R.id.bannerContainer), isTablet);

        setupWebView();
    }

    private void setupWindow() {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        hideSystemUI();
    }

    private boolean checkIsTablet() {
        return (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView = findViewById(R.id.webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new AdMobBridge(), "AndroidAdMob");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return !req.getUrl().toString().startsWith("file://");
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("file:///android_asset/www/index.html");
    }

    public class AdMobBridge {
        @android.webkit.JavascriptInterface
        public void showBanner() {
            runOnUiThread(() -> findViewById(R.id.bannerContainer).setVisibility(View.VISIBLE));
        }

        @android.webkit.JavascriptInterface
        public void hideBanner() {
            runOnUiThread(() -> findViewById(R.id.bannerContainer).setVisibility(View.GONE));
        }

        @android.webkit.JavascriptInterface
        public void showInterstitial() {
            gameCount++;
            if (gameCount % INTERSTITIAL_EVERY == 0) {
                runOnUiThread(() -> adsManager.showInterstitial());
            }
        }

        @android.webkit.JavascriptInterface
        public void showRewarded() {
            runOnUiThread(() -> adsManager.showRewarded(amount -> 
                webView.evaluateJavascript("window.onRewardGranted(" + amount + ")", null)
            ));
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        adsManager.onResume();
    }

    @Override
    protected void onPause() {
        adsManager.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        adsManager.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
