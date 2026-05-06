package uz.kidzone.app;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
        
        // Edge-to-edge support for modern Android
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        setContentView(R.layout.activity_main);

        // Apply insets to the main layout to prevent overlap with notches/system bars
        View mainLayout = findViewById(R.id.main_root);
        if (mainLayout != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, 0);
                View banner = findViewById(R.id.bannerContainer);
                if (banner != null) banner.setPadding(0, 0, 0, bars.bottom);
                return insets;
            });
        }

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
                findViewById(R.id.bannerContainer).setVisibility(View.GONE);
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
                View banner = findViewById(R.id.bannerContainer);
                if (banner != null && banner.getVisibility() == View.GONE) {
                    // Foydalanuvchi o'yin ichida — o'yindan chiqishni so'raymiz
                    showExitDialog(true);
                } else if (webViewManager != null && webViewManager.canGoBack()) {
                    webViewManager.goBack();
                } else {
                    // Foydalanuvchi asosiy menyuda — dasturdan chiqishni so'raymiz
                    showExitDialog(false);
                }
            }
        });
    }

    private void showExitDialog(boolean isInGame) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_exit, null);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.Theme_KidZone_Dialog)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        android.widget.TextView title = dialogView.findViewById(R.id.dialog_title);
        android.widget.TextView msg   = dialogView.findViewById(R.id.dialog_message);
        android.widget.TextView emoji = dialogView.findViewById(R.id.dialog_emoji);
        MaterialButton btnYes = dialogView.findViewById(R.id.btn_yes);
        MaterialButton btnNo  = dialogView.findViewById(R.id.btn_no);

        if (isInGame) {
            title.setText(getTranslatedString("exit_title",   "Exit Game?"));
            msg.setText(getTranslatedString("exit_msg",       "Return to menu? Your progress might not be saved."));
            btnYes.setText(getTranslatedString("yes_menu",    "Yes, menu"));
        } else {
            title.setText(getTranslatedString("app_exit_title", "Exit KidZone?"));
            msg.setText(getTranslatedString("app_exit_msg",     "Are you sure? We will miss you!"));
            btnYes.setText(getTranslatedString("yes_exit",      "Yes, exit"));
        }
        btnNo.setText(getTranslatedString("no_stay", "No, stay"));

        // Emoji bounce animation on dialog open
        emoji.setScaleX(0.2f);
        emoji.setScaleY(0.2f);
        emoji.setAlpha(0f);
        emoji.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(480)
                .setInterpolator(new android.view.animation.OvershootInterpolator(2.8f))
                .start();

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            if (isInGame) {
                webViewManager.evaluateJavascript("if(window.app) app.closeGame();");
            } else {
                finishAffinity();
            }
        });
        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private String getTranslatedString(String key, String defaultVal) {
        if (webViewManager == null || webViewManager.getLanguage() == null) return defaultVal;

        String lang = webViewManager.getLanguage();
        if ("uz".equals(lang)) {
            switch (key) {
                case "exit_title":     return "O'yindan chiqish?";
                case "exit_msg":       return "Menyuga qaytmoqchimisiz? Ballaringiz saqlanmasligi mumkin.";
                case "app_exit_title": return "KidZone'dan chiqish?";
                case "app_exit_msg":   return "Haqiqatan ham chiqib ketmoqchimisiz? Sizni sog'inib qolamiz!";
                case "yes_exit":       return "Ha, chiqish";
                case "yes_menu":       return "Ha, menyuga";
                case "no_stay":        return "Yo'q, qolish";
            }
        } else if ("ru".equals(lang)) {
            switch (key) {
                case "exit_title":     return "Выйти из игры?";
                case "exit_msg":       return "Вернуться в меню? Ваши баллы могут не сохраниться.";
                case "app_exit_title": return "Выйти из KidZone?";
                case "app_exit_msg":   return "Вы действительно хотите уйти? Мы будем скучать!";
                case "yes_exit":       return "Да, выйти";
                case "yes_menu":       return "Да, в меню";
                case "no_stay":        return "Нет, остаться";
            }
        } else {
            switch (key) {
                case "yes_exit":  return "Yes, exit";
                case "yes_menu":  return "Yes, menu";
                case "no_stay":   return "No, stay";
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
                webViewManager.evaluateJavascript("if(window.updateBannerOffset) updateBannerOffset(" + lastBannerHeight + ");");
            });
        }

        @JavascriptInterface
        public void hideBanner() {
            runOnUiThread(() -> {
                findViewById(R.id.bannerContainer).setVisibility(View.GONE);
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
