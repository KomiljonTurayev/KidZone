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

    static java.lang.ref.WeakReference<MainActivity> instance;

    private KidWebViewManager webViewManager;
    private IAdsManager adsManager;
    private SystemUiHelper systemUiHelper;
    private BirdAiManager birdAiManager;
    private int lastBannerHeight = 0;
    private int gameLaunchCount = 0;
    private android.content.SharedPreferences kzPrefs;
    private ParentalStatsManager statsManager;
    private android.os.Handler timeLockHandler;
    private View lockOverlay;

    private final Runnable timeLockRunnable = new Runnable() {
        @Override public void run() {
            if (statsManager != null && statsManager.isTimeLimitReached()) {
                showLockOverlay();
            } else {
                timeLockHandler.postDelayed(this, 60_000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        kzPrefs = getSharedPreferences(OnboardingActivity.PREFS, MODE_PRIVATE);
        if (!kzPrefs.getBoolean(OnboardingActivity.KEY_DONE, false)) {
            startActivity(new android.content.Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        instance = new java.lang.ref.WeakReference<>(this);
        FirebaseManager.init(this);
        statsManager = new ParentalStatsManager(this);
        timeLockHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        initializeUI();
        initializeManagers();
        setupKidzoFab();
    }

    private void setupKidzoFab() {
        birdAiManager = new BirdAiManager(this); // kept for onDestroy lifecycle

        View fab = findViewById(R.id.btnBirdAi);
        if (fab == null) return;
        fab.bringToFront();

        android.animation.ObjectAnimator scaleX =
            android.animation.ObjectAnimator.ofFloat(fab, "scaleX", 1f, 1.18f);
        android.animation.ObjectAnimator scaleY =
            android.animation.ObjectAnimator.ofFloat(fab, "scaleY", 1f, 1.18f);
        scaleX.setDuration(900);
        scaleX.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
        scaleX.setRepeatMode(android.animation.ObjectAnimator.REVERSE);
        scaleY.setDuration(900);
        scaleY.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
        scaleY.setRepeatMode(android.animation.ObjectAnimator.REVERSE);
        android.animation.AnimatorSet pulse = new android.animation.AnimatorSet();
        pulse.playTogether(scaleX, scaleY);
        pulse.start();

        try {
            uz.kidzone.app.kidzo.ContentFilter contentFilter =
                uz.kidzone.app.kidzo.ContentFilter.fromAssets(this);
            uz.kidzone.app.kidzo.KidzoAgent kidzoAgent =
                uz.kidzone.app.kidzo.KidzoAgent.createStatic(contentFilter, this::runOnUiThread);

            fab.setOnClickListener(v -> {
                uz.kidzone.app.kidzo.KidzoBottomSheet sheet =
                    new uz.kidzone.app.kidzo.KidzoBottomSheet(kidzoAgent, contentId -> {
                        String safeId = contentId.replaceAll("[^a-zA-Z0-9\\-]", "");
                        webViewManager.evaluateJavascript("if(window.playContent)playContent('" + safeId + "')");
                    });
                sheet.show(getSupportFragmentManager(), "kidzo");
                kidzoAgent.requestRecommendations();
            });
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Kidzo init failed", e);
        }
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
                // ✅ Professional Fix: Apply top and bottom system bar insets to the root container
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
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
                webViewManager.evaluateJavascript("if(window.updateBannerOffset) updateBannerOffset(0);");
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

        final String lang = kzPrefs.getString(OnboardingActivity.KEY_LANG, "uz");
        final String age  = kzPrefs.getString(OnboardingActivity.KEY_AGE,  "2-4");
        webViewManager.setOnPageReadyCallback(() ->
            webViewManager.evaluateJavascript(
                "localStorage.setItem('kz-lang','" + lang + "');" +
                "localStorage.setItem('kz-age','"  + age  + "');")
        );

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
                webViewManager.evaluateJavascript("if(window.updateBannerOffset) updateBannerOffset(0);");
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

        @JavascriptInterface
        public void openParentalDashboard() {
            runOnUiThread(() -> showPinDialog());
        }

        @JavascriptInterface
        public void gameLaunched(String gameId) {
            if (statsManager != null) statsManager.onGameLaunched(gameId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        systemUiHelper.enableImmersiveMode();
        if (adsManager != null) adsManager.onResume();
        MusicManager.getInstance().startMusic(this);
        if (statsManager != null) {
            statsManager.onSessionStart();
            timeLockHandler.postDelayed(timeLockRunnable, 60_000);
        }
    }

    @Override
    protected void onPause() {
        if (statsManager != null) {
            statsManager.onSessionEnd();
            timeLockHandler.removeCallbacks(timeLockRunnable);
        }
        if (adsManager != null) adsManager.onPause();
        MusicManager.getInstance().pauseMusic();
        super.onPause();
    }

    void injectJs(String script) {
        if (webViewManager != null) webViewManager.evaluateJavascript(script);
    }

    public void evaluateJs(String script, android.webkit.ValueCallback<String> cb) {
        if (webViewManager != null) webViewManager.evaluateJavascript(script, cb);
    }

    private void showPinDialog() {
        String hash = PinUtil.getOrMigrateHash(kzPrefs, "kz_pin");
        if (hash == null) {
            PinDialogHelper.showCreate(this, pin -> {
                kzPrefs.edit().putString("kz_pin", pin.isEmpty() ? "" : PinUtil.hash(pin)).apply();
                openDashboard();
            });
        } else if (hash.isEmpty()) {
            openDashboard();
        } else {
            PinDialogHelper.showEnter(this, hash, this::openDashboard);
        }
    }

    private void openDashboard() {
        startActivity(new android.content.Intent(this, ParentalDashboardActivity.class));
    }

    private void showLockOverlay() {
        if (lockOverlay != null) return;
        timeLockHandler.removeCallbacks(timeLockRunnable);

        lockOverlay = getLayoutInflater().inflate(R.layout.view_lock_overlay, null);

        String lang = kzPrefs.getString(OnboardingActivity.KEY_LANG, "uz");
        android.widget.TextView tvTitle = lockOverlay.findViewById(R.id.lock_title);
        android.widget.TextView tvSub   = lockOverlay.findViewById(R.id.lock_subtitle);
        if ("ru".equals(lang)) {
            tvTitle.setText("Время вышло! ⏰");
            tvSub.setText("Введите PIN для продолжения");
        } else if ("en".equals(lang)) {
            tvTitle.setText("Time's up! ⏰");
            tvSub.setText("Enter parent PIN to continue");
        }

        String savedPinHash = PinUtil.getOrMigrateHash(kzPrefs, "kz_pin");
        View pinSection  = lockOverlay.findViewById(R.id.lock_pin_section);
        View continueBtn = lockOverlay.findViewById(R.id.lock_continue);

        if (savedPinHash == null || savedPinHash.isEmpty()) {
            pinSection.setVisibility(View.GONE);
            continueBtn.setVisibility(View.VISIBLE);
            continueBtn.setOnClickListener(v -> hideLockOverlay());
        } else {
            pinSection.setVisibility(View.VISIBLE);
            continueBtn.setVisibility(View.GONE);
            wireLockPad(lockOverlay, savedPinHash);
        }

        android.widget.FrameLayout decor =
            (android.widget.FrameLayout) getWindow().getDecorView();
        decor.addView(lockOverlay, new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void hideLockOverlay() {
        if (lockOverlay == null) return;
        if (statsManager != null) statsManager.setTimeLimitMinutes(0);
        ((android.widget.FrameLayout) getWindow().getDecorView()).removeView(lockOverlay);
        lockOverlay = null;
        timeLockHandler.postDelayed(timeLockRunnable, 60_000);
    }

    private void wireLockPad(View overlay, String expectedHash) {
        View[] dots = {
            overlay.findViewById(R.id.lock_dot1), overlay.findViewById(R.id.lock_dot2),
            overlay.findViewById(R.id.lock_dot3), overlay.findViewById(R.id.lock_dot4)
        };
        View dotsContainer = overlay.findViewById(R.id.lock_dots);
        StringBuilder entered = new StringBuilder();

        int[] keyIds = {R.id.lock_key_1, R.id.lock_key_2, R.id.lock_key_3,
                        R.id.lock_key_4, R.id.lock_key_5, R.id.lock_key_6,
                        R.id.lock_key_7, R.id.lock_key_8, R.id.lock_key_9,
                        R.id.lock_key_0};
        String[] digits = {"1","2","3","4","5","6","7","8","9","0"};

        for (int i = 0; i < keyIds.length; i++) {
            final String d = digits[i];
            overlay.findViewById(keyIds[i]).setOnClickListener(v -> {
                if (entered.length() >= 4) return;
                entered.append(d);
                updateLockDots(dots, entered.length());
                if (entered.length() == 4) {
                    if (PinUtil.matches(entered.toString(), expectedHash)) {
                        hideLockOverlay();
                    } else {
                        android.animation.ObjectAnimator anim =
                            android.animation.ObjectAnimator.ofFloat(dotsContainer, "translationX",
                                0f, -10f, 10f, -10f, 10f, -5f, 5f, 0f);
                        anim.setDuration(400);
                        anim.start();
                        entered.setLength(0);
                        updateLockDots(dots, 0);
                    }
                }
            });
        }

        overlay.findViewById(R.id.lock_key_bsp).setOnClickListener(v -> {
            if (entered.length() > 0) {
                entered.deleteCharAt(entered.length() - 1);
                updateLockDots(dots, entered.length());
            }
        });
    }

    private static void updateLockDots(View[] dots, int count) {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackgroundResource(
                i < count ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
        }
    }

    @Override
    protected void onDestroy() {
        instance = null;
        if (adsManager != null) adsManager.onDestroy();
        if (webViewManager != null) webViewManager.destroy();
        if (birdAiManager != null) birdAiManager.onDestroy();
        super.onDestroy();
    }
}
