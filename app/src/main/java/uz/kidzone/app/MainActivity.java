package uz.kidzone.app;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.ads.mediation.admob.AdMobAdapter;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "KidZone";

    // ═══════════════════════════════════════════════════════════════
    // ADMOB ID LAR — AdMob konsoldagi haqiqiy ID lar bilan ALMASHTIRING
    // Test ID lardan productionga chiqargunga qadar foydalanish mumkin
    // ═══════════════════════════════════════════════════════════════

    // PRODUCTION uchun:
    private static final String BANNER_ID       = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX";
    private static final String INTERSTITIAL_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX";
    private static final String REWARDED_ID     = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX";

    // TEST ID lar (faqat development uchun):
    // private static final String BANNER_ID       = "ca-app-pub-3940256099942544/6300978111";
    // private static final String INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";
    // private static final String REWARDED_ID     = "ca-app-pub-3940256099942544/5224354917";

    private WebView webView;
    private AdView bannerAdView;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ═══════════════════════════════════════════════════════════
        // 1. ADMOB CHILD-DIRECTED CONFIGURATION (COPPA + Families Policy)
        // Bu KidZone uchun MAJBURIY — bolalar ilovasi
        // ═══════════════════════════════════════════════════════════
        configureAdMobForChildren();

        // 2. AdMob initialization
        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "AdMob initialized for child-directed treatment");
            loadBannerAd();
            loadInterstitialAd();
            loadRewardedAd();
        });

        // 3. WebView setup
        setupWebView();
    }

    /**
     * KidZone uchun — barcha reklamalar bolalarga yo'naltirilgan deb
     * belgilanadi. Bu COPPA va Google Play Families Policy talabi.
     */
    private void configureAdMobForChildren() {
        RequestConfiguration requestConfiguration = new RequestConfiguration.Builder()
                // Bolalarga yo'naltirilgan deb belgilash
                .setTagForChildDirectedTreatment(
                        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
                )
                // Reklama yoshga mos bo'lishi (G = General, hamma uchun)
                .setMaxAdContentRating(
                        RequestConfiguration.MAX_AD_CONTENT_RATING_G
                )
                // GDPR uchun yoshi 16 dan kichik foydalanuvchilarga moslash
                .setTagForUnderAgeOfConsent(
                        RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
                )
                // Test qurilmalar (development uchun, productionda bo'sh qoldiring)
                .setTestDeviceIds(Arrays.asList())
                .build();

        MobileAds.setRequestConfiguration(requestConfiguration);
    }

    /**
     * Non-personalized ad request yaratish.
     * Bolalar uchun shaxsiylashtirilmagan reklama MAJBURIY.
     */
    private AdRequest createNonPersonalizedAdRequest() {
        Bundle extras = new Bundle();
        extras.putString("npa", "1"); // Non-Personalized Ads = enabled

        return new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // BANNER AD
    // ═══════════════════════════════════════════════════════════════
    private void loadBannerAd() {
        FrameLayout adContainer = findViewById(R.id.banner_container);
        if (adContainer == null) {
            Log.e(TAG, "banner_container not found in layout");
            return;
        }

        bannerAdView = new AdView(this);
        bannerAdView.setAdUnitId(BANNER_ID);
        bannerAdView.setAdSize(getAdaptiveBannerSize());

        adContainer.addView(bannerAdView);
        bannerAdView.loadAd(createNonPersonalizedAdRequest());

        Log.d(TAG, "Banner ad request sent (non-personalized)");
    }

    /**
     * Tablet va telefon uchun adaptive banner o'lchami
     */
    private AdSize getAdaptiveBannerSize() {
        int adWidthPixels = getResources().getDisplayMetrics().widthPixels;
        float density = getResources().getDisplayMetrics().density;
        int adWidth = (int) (adWidthPixels / density);

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    // ═══════════════════════════════════════════════════════════════
    // INTERSTITIAL AD
    // ═══════════════════════════════════════════════════════════════
    private void loadInterstitialAd() {
        InterstitialAd.load(this, INTERSTITIAL_ID, createNonPersonalizedAdRequest(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                        Log.d(TAG, "Interstitial ad loaded");

                        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                interstitialAd = null;
                                loadInterstitialAd(); // Keyingi reklama uchun yangidan yuklash
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                interstitialAd = null;
                                Log.e(TAG, "Interstitial failed: " + adError.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        interstitialAd = null;
                        Log.e(TAG, "Interstitial load failed: " + loadAdError.getMessage());
                    }
                });
    }

    private void showInterstitialAd() {
        if (interstitialAd != null) {
            interstitialAd.show(this);
        } else {
            Log.d(TAG, "Interstitial not ready yet");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // REWARDED AD
    // ═══════════════════════════════════════════════════════════════
    private void loadRewardedAd() {
        RewardedAd.load(this, REWARDED_ID, createNonPersonalizedAdRequest(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d(TAG, "Rewarded ad loaded");

                        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                rewardedAd = null;
                                loadRewardedAd();
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        rewardedAd = null;
                        Log.e(TAG, "Rewarded load failed: " + loadAdError.getMessage());
                    }
                });
    }

    private void showRewardedAd() {
        if (rewardedAd != null) {
            rewardedAd.show(this, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(com.google.android.gms.ads.rewarded.RewardItem rewardItem) {
                    // Reward berildi — JS ga xabar yuborish
                    runOnUiThread(() -> webView.evaluateJavascript(
                            "window.onRewardEarned && window.onRewardEarned();", null));
                }
            });
        } else {
            Toast.makeText(this, "Reklama tayyor emas, biroz kuting", Toast.LENGTH_SHORT).show();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // WEBVIEW + JS BRIDGE
    // ═══════════════════════════════════════════════════════════════
    private void setupWebView() {
        webView = findViewById(R.id.webview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AdBridge(this), "AndroidAd");

        webView.loadUrl("file:///android_asset/www/index.html");
    }

    /**
     * JS dan Java ga ko'prik — HTML5 o'yinlar reklama chaqirishi uchun
     *
     * Misol (HTML/JS dan ishlatish):
     *   AndroidAd.showInterstitial();
     *   AndroidAd.showRewarded();
     */
    public class AdBridge {
        Context context;

        AdBridge(Context c) { this.context = c; }

        @JavascriptInterface
        public void showInterstitial() {
            new Handler(Looper.getMainLooper()).post(() -> showInterstitialAd());
        }

        @JavascriptInterface
        public void showRewarded() {
            new Handler(Looper.getMainLooper()).post(() -> showRewardedAd());
        }
    }

    @Override
    protected void onPause() {
        if (bannerAdView != null) bannerAdView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerAdView != null) bannerAdView.resume();
    }

    @Override
    protected void onDestroy() {
        if (bannerAdView != null) bannerAdView.destroy();
        super.onDestroy();
    }
}
