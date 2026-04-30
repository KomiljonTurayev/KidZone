package uz.kidzone.app;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

/**
 * Main Activity of the KidZone application.
 * Manages the lifecycle of WebView and Advertisements.
 * Follows Clean Architecture by delegating responsibilities to Managers.
 */
public class MainActivity extends AppCompatActivity {

    private static final String JS_INTERFACE_NAME = "AndroidAdMob";
    private static final String INDEX_PATH = "file:///android_asset/www/index.html";
    private static final int INTERSTITIAL_FREQUENCY = 3;

    private KidWebViewManager webViewManager;
    private IAdsManager adsManager;
    private SystemUiHelper systemUiHelper;
    private int lastBannerHeight = 0;
    private int gameLaunchCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        initializeUI();
        initializeManagers();
    }

    private void initializeUI() {
        // Keep screen on for games
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        systemUiHelper = new SystemUiHelper(getWindow());
        systemUiHelper.enableImmersiveMode();
    }

    private void initializeManagers() {
        // Ads Management
        adsManager = new AdsManager(this);
        adsManager.initialize();
        
        ((AdsManager) adsManager).loadBanner(findViewById(R.id.bannerContainer), isTabletDevice(), new IAdsManager.BannerListener() {
            @Override
            public void onBannerLoaded(int heightDp) {
                lastBannerHeight = heightDp;
                webViewManager.evaluateJavascript("if(window.updateBannerOffset) updateBannerOffset(" + heightDp + ");");
            }

            @Override
            public void onBannerFailed() {
                lastBannerHeight = 0;
                webViewManager.evaluateJavascript("if(window.updateBannerOffset) updateBannerOffset(0);");
            }
        });

        // WebView Management
        webViewManager = new KidWebViewManager(findViewById(R.id.webView));
        webViewManager.setup(new AdMobBridge(), JS_INTERFACE_NAME);
        webViewManager.loadUrl(INDEX_PATH);

        setupNavigation();
    }

    private void setupNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Agar reklama banneri yashirin bo'lsa, demak foydalanuvchi o'yin ichida
                View banner = findViewById(R.id.bannerContainer);
                if (banner != null && banner.getVisibility() == View.GONE) {
                    showExitConfirmation();
                } else if (webViewManager != null && webViewManager.canGoBack()) {
                    webViewManager.goBack();
                } else {
                    setEnabled(false);
                    MainActivity.super.onBackPressed();
                }
            }
        });
    }

    private void showExitConfirmation() {
        // Professional Material 3 themed dialog
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_KidZone_Dialog)
                .setTitle(getTranslatedString("exit_title", "Exit Game"))
                .setMessage(getTranslatedString("exit_msg", "Do you really want to exit? Your progress for this session might not be saved."))
                .setPositiveButton(getTranslatedString("yes", "Yes"), (d, which) -> webViewManager.evaluateJavascript("if(window.app) app.closeGame();"))
                .setNegativeButton(getTranslatedString("no", "No"), null)
                .create();
        
        dialog.show();
        
        // Style buttons after show
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.md_primary, getTheme()));
    }

    private String getTranslatedString(String key, String defaultVal) {
        // Dynamic translation based on current web language
        if (webViewManager == null || webViewManager.getLanguage() == null) return defaultVal;
        
        String lang = webViewManager.getLanguage();
        // This is a simplified mapper. In a larger app, you'd use localized strings.xml
        if ("uz".equals(lang)) {
            switch(key) {
                case "exit_title": return "O'yinni tark etish";
                case "exit_msg": return "Haqiqatan ham o'yindan chiqmoqchimisiz? To'plangan ballaringiz saqlanmasligi mumkin.";
                case "yes": return "Ha";
                case "no": return "Yo'q";
            }
        } else if ("ru".equals(lang)) {
            switch(key) {
                case "exit_title": return "Выход из игры";
                case "exit_msg": return "Вы действительно хотите выйти? Ваши баллы могут не сохраниться.";
                case "yes": return "Да";
                case "no": return "Нет";
            }
        }
        return defaultVal;
    }

    private boolean isTabletDevice() {
        return (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * JavaScript Interface for communicating between WebView and Native Android.
     */
    private class AdMobBridge {
        @JavascriptInterface
        public void showBanner() {
            runOnUiThread(() -> {
                findViewById(R.id.bannerContainer).setVisibility(View.VISIBLE);
                // Bannerni qayta ko'rsatganda oxirgi olingan balandlikni qo'llaymiz
                webViewManager.evaluateJavascript("if(window.updateBannerOffset) updateBannerOffset(" + lastBannerHeight + ");");
            });
        }

        @JavascriptInterface
        public void hideBanner() {
            runOnUiThread(() -> {
                findViewById(R.id.bannerContainer).setVisibility(View.GONE);
                // UI pastga tushishi uchun offsetni 0 qilamiz
                webViewManager.evaluateJavascript("if(window.updateBannerOffset) updateBannerOffset(0);");
            });
        }

        @JavascriptInterface
        public void showInterstitial() {
            gameLaunchCount++;
            if (gameLaunchCount % INTERSTITIAL_FREQUENCY == 0) {
                runOnUiThread(() -> adsManager.showInterstitial());
            }
        }

        @JavascriptInterface
        public void showRewarded() {
            runOnUiThread(() -> adsManager.showRewarded(amount -> 
                webViewManager.evaluateJavascript("window.onRewardGranted(" + amount + ")")
            ));
        }

        @JavascriptInterface
        public void toggleMusic(boolean mute) {
            runOnUiThread(() -> MusicManager.getInstance().setMuted(mute));
        }

        @JavascriptInterface
        public void updateLanguage(String lang) {
            runOnUiThread(() -> {
                if (webViewManager != null) webViewManager.setLanguage(lang);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        systemUiHelper.enableImmersiveMode();
        if (adsManager != null) adsManager.onResume();
        MusicManager.getInstance().startMusic(this);
    }

    @Override
    protected void onPause() {
        if (adsManager != null) adsManager.onPause();
        MusicManager.getInstance().pauseMusic();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adsManager != null) adsManager.onDestroy();
        if (webViewManager != null) webViewManager.destroy();
        super.onDestroy();
    }
}
