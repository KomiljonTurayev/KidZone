package uz.kidzone.app;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "KidZone";



    // ══════════════════════════════════════════════════════
    // ✏️  ADMOB ID LAR — strings.xml dan o'qiladi
    // ══════════════════════════════════════════════════════
    // strings.xml da:
    //   <string name="banner_ad_unit_id">ca-app-pub-XXXX/ID_1</string>
    //   <string name="interstitial_ad_unit_id">ca-app-pub-XXXX/ID_2</string>
    //   <string name="rewarded_ad_unit_id">ca-app-pub-XXXX/ID_3</string>
    //
    // TEST UCHUN (release da o'z ID laringizni strings.xml ga yozing):
    //   banner:        ca-app-pub-3940256099942544/6300978111
    //   interstitial:  ca-app-pub-3940256099942544/1033173712
    //   rewarded:      ca-app-pub-3940256099942544/5224354917
    // ══════════════════════════════════════════════════════

    // Interstitial har necha o'yinda bir marta chiqadi
    private static final int INTERSTITIAL_EVERY = 3;

    // View lar
    private WebView       webView;
    private AdView        bannerAdView;
    private LinearLayout  bannerContainer;

    // AdMob ob'ektlari
    private InterstitialAd interstitialAd;
    private RewardedAd     rewardedAd;

    // Holat
    private int     gameCount = 0;
    private boolean isTablet  = false;
    private boolean bannerLoaded = false;

    // ── ONCREATE ──────────────────────────────────────────
    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // To'liq ekran
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        hideSystemUI();

        // Planshet yoki telefon
        isTablet = (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;

        setContentView(R.layout.activity_main);
        bannerContainer = findViewById(R.id.bannerContainer);

        // AdMob — COPPA (bolalar ilovasi uchun MAJBURIY)
        RequestConfiguration config = new RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(
                        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setTagForUnderAgeOfConsent(
                        RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
                .setMaxAdContentRating(
                        RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build();
        MobileAds.setRequestConfiguration(config);

        MobileAds.initialize(this, initStatus -> {
            Log.d(TAG, "AdMob initialized ✅");
            loadBanner();
            loadInterstitial();
            loadRewarded();
        });

        setupWebView();
    }

    // ── WEBVIEW ───────────────────────────────────────────
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView = findViewById(R.id.webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(false);

        // JavaScript ↔ Java ko'prik
        webView.addJavascriptInterface(new AdMobBridge(), "AndroidAdMob");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (url.startsWith("file://")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("file:///android_asset/www/index.html");
    }

    // ══════════════════════════════════════════════════════
    // ADMOB BRIDGE  —  JavaScript → Java
    // HTML dan: AndroidAdMob.showBanner() va h.k.
    // ══════════════════════════════════════════════════════
    public class AdMobBridge {

        // ── showBanner() ──────────────────────────────────
        // O'yin yopilganda yoki sahifa ochilganda chaqiriladi
        @android.webkit.JavascriptInterface
        public void showBanner() {
            runOnUiThread(() -> {
                if (bannerAdView != null) {
                    bannerContainer.setVisibility(View.VISIBLE);
                    bannerAdView.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Banner ko'rsatildi ✅");
                } else {
                    Log.w(TAG, "Banner hali yuklanmagan, qayta urinilmoqda...");
                    loadBanner();
                }
            });
        }

        // ── hideBanner() ──────────────────────────────────
        // O'yin ochilganda (to'liq ekran)
        @android.webkit.JavascriptInterface
        public void hideBanner() {
            runOnUiThread(() -> {
                if (bannerAdView != null) {
                    bannerContainer.setVisibility(View.GONE);
                    Log.d(TAG, "Banner yashirildi");
                }
            });
        }

        // ── showInterstitial() ────────────────────────────
        // Har INTERSTITIAL_EVERY o'yinda bir marta chiqadi
        @android.webkit.JavascriptInterface
        public void showInterstitial() {
            gameCount++;
            Log.d(TAG, "O'yin #" + gameCount);
            if (gameCount % INTERSTITIAL_EVERY != 0) return;

            runOnUiThread(() -> {
                if (interstitialAd != null) {
                    interstitialAd.show(MainActivity.this);
                    interstitialAd = null;
                    // Ko'rsatilgandan keyin yangisini yuklash
                    new Handler(Looper.getMainLooper())
                            .postDelayed(() -> loadInterstitial(), 2000);
                    Log.d(TAG, "Interstitial ko'rsatildi ✅");
                } else {
                    Log.w(TAG, "Interstitial tayyor emas, yuklanmoqda...");
                    loadInterstitial();
                }
            });
        }

        // ── showRewarded() ────────────────────────────────
        // "Bonus yulduz ol!" tugmasi bosilganda
        // Mukofot berilsa: window.onRewardGranted(miqdor) chaqiriladi
        @android.webkit.JavascriptInterface
        public void showRewarded() {
            runOnUiThread(() -> {
                if (rewardedAd != null) {
                    rewardedAd.show(MainActivity.this, rewardItem -> {
                        int amount = rewardItem.getAmount();
                        Log.d(TAG, "Mukofot berildi: " + amount);
                        // JS ga xabar berish
                        webView.evaluateJavascript(
                                "window.onRewardGranted && window.onRewardGranted(" + amount + ")",
                                null
                        );
                    });
                    rewardedAd = null;
                    new Handler(Looper.getMainLooper())
                            .postDelayed(() -> loadRewarded(), 3000);
                } else {
                    Log.w(TAG, "Rewarded tayyor emas, yuklanmoqda...");
                    loadRewarded();
                    // JS ga xabar: reklama yo'q
                    webView.evaluateJavascript(
                            "window.showToast && window.showToast('⏳ Reklama yuklanmoqda, qayta urining')",
                            null
                    );
                }
            });
        }

        // ── isTablet() ────────────────────────────────────
        @android.webkit.JavascriptInterface
        public boolean isTablet() {
            return isTablet;
        }

        // ── gameOpened(id) ────────────────────────────────
        @android.webkit.JavascriptInterface
        public void gameOpened(String gameId) {
            Log.d(TAG, "O'yin ochildi: " + gameId);
        }
    }

    // ══════════════════════════════════════════════════════
    // BANNER YUKLASH
    // ══════════════════════════════════════════════════════
    private void loadBanner() {
        if (bannerAdView != null) {
            bannerContainer.removeView(bannerAdView);
            bannerAdView.destroy();
        }

        bannerAdView = new AdView(this);
        // Planshetda katta (728×90), telefonda kichik (320×50)
        bannerAdView.setAdSize(isTablet ? AdSize.LEADERBOARD : AdSize.BANNER);
        bannerAdView.setAdUnitId(getString(R.string.banner_ad_unit_id));

        bannerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                bannerLoaded = true;
                bannerContainer.setVisibility(View.VISIBLE);
                bannerAdView.setVisibility(View.VISIBLE);
                Log.d(TAG, "Banner yuklandi ✅");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError e) {
                bannerLoaded = false;
                bannerContainer.setVisibility(View.GONE);
                Log.w(TAG, "Banner xatosi: " + e.getMessage());
                // 30 sekunddan keyin qayta urinish
                new Handler(Looper.getMainLooper())
                        .postDelayed(() -> loadBanner(), 30_000);
            }

            @Override
            public void onAdClicked() {
                Log.d(TAG, "Banner bosildi");
            }
        });

        bannerContainer.addView(bannerAdView);
        bannerAdView.loadAd(buildAdRequest());
    }

    // ══════════════════════════════════════════════════════
    // INTERSTITIAL YUKLASH
    // ══════════════════════════════════════════════════════
    private void loadInterstitial() {
        InterstitialAd.load(
                this,
                getString(R.string.interstitial_ad_unit_id),
                buildAdRequest(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                        Log.d(TAG, "Interstitial yuklandi ✅");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError e) {
                        interstitialAd = null;
                        Log.w(TAG, "Interstitial xatosi: " + e.getMessage());
                    }
                }
        );
    }

    // ══════════════════════════════════════════════════════
    // REWARDED YUKLASH
    // ══════════════════════════════════════════════════════
    private void loadRewarded() {
        RewardedAd.load(
                this,
                getString(R.string.rewarded_ad_unit_id),
                buildAdRequest(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d(TAG, "Rewarded yuklandi ✅");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError e) {
                        rewardedAd = null;
                        Log.w(TAG, "Rewarded xatosi: " + e.getMessage());
                    }
                }
        );
    }

    // ── AD REQUEST ────────────────────────────────────────
    private AdRequest buildAdRequest() {
        return new AdRequest.Builder().build();
    }

    // ── TO'LIQ EKRAN ──────────────────────────────────────
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

    // ── LIFECYCLE ─────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (bannerAdView != null) bannerAdView.resume();
    }

    @Override
    protected void onPause() {
        if (bannerAdView != null) bannerAdView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (bannerAdView != null) {
            bannerAdView.destroy();
            bannerAdView = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
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