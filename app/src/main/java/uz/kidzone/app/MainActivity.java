package uz.kidzone.app;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class MainActivity extends AppCompatActivity {

    private static final int INTERSTITIAL_EVERY = 3;
    private static final String JS_INTERFACE_NAME = "AndroidAdMob";
    private static final String INDEX_PATH = "file:///android_asset/www/index.html";

    private KidWebViewManager webViewManager;
    private IAdsManager adsManager;
    private int gameCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        setupWindow();
        setContentView(R.layout.activity_main);

        initManagers();
    }

    private void setupWindow() {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        hideSystemUI();
    }

    private void initManagers() {
        boolean isTablet = checkIsTablet();
        adsManager = new AdsManager(this);
        adsManager.loadBanner(findViewById(R.id.bannerContainer), isTablet);

        webViewManager = new KidWebViewManager(findViewById(R.id.webView));
        webViewManager.setup(new AdMobBridge(), JS_INTERFACE_NAME);
        webViewManager.loadUrl(INDEX_PATH);

        setupBackPressed();
    }

    private void setupBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webViewManager != null && webViewManager.canGoBack()) {
                    webViewManager.goBack();
                } else {
                    setEnabled(false);
                    MainActivity.super.onBackPressed();
                }
            }
        });
    }

    private boolean checkIsTablet() {
        return (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
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

    public class AdMobBridge {
        @JavascriptInterface
        public void showBanner() {
            runOnUiThread(() -> findViewById(R.id.bannerContainer).setVisibility(View.VISIBLE));
        }

        @JavascriptInterface
        public void hideBanner() {
            runOnUiThread(() -> findViewById(R.id.bannerContainer).setVisibility(View.GONE));
        }

        @JavascriptInterface
        public void showInterstitial() {
            gameCount++;
            if (gameCount % INTERSTITIAL_EVERY == 0) {
                runOnUiThread(() -> adsManager.showInterstitial());
            }
        }

        @JavascriptInterface
        public void showRewarded() {
            runOnUiThread(() -> adsManager.showRewarded(amount -> 
                webViewManager.evaluateJavascript("window.onRewardGranted(" + amount + ")")
            ));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (adsManager != null) adsManager.onResume();
    }

    @Override
    protected void onPause() {
        if (adsManager != null) adsManager.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adsManager != null) adsManager.onDestroy();
        if (webViewManager != null) webViewManager.destroy();
        super.onDestroy();
    }


}
