package uz.kidzone.app;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

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

/**
 * Implementation of IAdsManager using Google Mobile Ads (AdMob).
 * Adheres to SRP by focusing only on Advertisement lifecycle.
 */
public class AdsManager implements IAdsManager {
    private static final String TAG = "AdsManager";
    private static final int BANNER_RETRY_DELAY_MS = 30000;

    private final Activity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private BannerListener bannerListener;
    private AdView bannerAdView;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;

    public AdsManager(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void initialize() {
        configureAdMob();
        MobileAds.initialize(activity, status -> {
            Log.d(TAG, "AdMob Initialized");
            loadInterstitial();
            loadRewarded();
        });
    }

    private void configureAdMob() {
        RequestConfiguration config = new RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build();
        MobileAds.setRequestConfiguration(config);
    }

    @Override
    public void loadBanner(ViewGroup container, boolean isTablet) {
        loadBanner(container, isTablet, null);
    }

    public void loadBanner(ViewGroup container, boolean isTablet, BannerListener listener) {
        cleanupBanner(container);
        this.bannerListener = listener;
        
        bannerAdView = new AdView(activity);
        AdSize adSize = isTablet ? AdSize.LEADERBOARD : AdSize.BANNER;
        bannerAdView.setAdSize(adSize);
        bannerAdView.setAdUnitId(activity.getString(R.string.banner_ad_unit_id));

        bannerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                container.setVisibility(View.VISIBLE);
                if (bannerListener != null) bannerListener.onBannerLoaded(adSize.getHeight());
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError e) {
                Log.e(TAG, "Banner failed: " + e.getMessage());
                if (bannerListener != null) bannerListener.onBannerFailed();
                mainHandler.postDelayed(() -> loadBanner(container, isTablet), BANNER_RETRY_DELAY_MS);
            }
        });

        container.addView(bannerAdView);
        bannerAdView.loadAd(createAdRequest());
    }

    private void cleanupBanner(ViewGroup container) {
        if (bannerAdView != null) {
            container.removeView(bannerAdView);
            bannerAdView.destroy();
            bannerAdView = null;
        }
    }

    @Override
    public void showInterstitial() {
        if (interstitialAd != null) {
            interstitialAd.show(activity);
            interstitialAd = null;
            loadInterstitial();
        } else {
            Log.d(TAG, "Interstitial not ready yet");
            loadInterstitial();
        }
    }

    private void loadInterstitial() {
        InterstitialAd.load(activity, activity.getString(R.string.interstitial_ad_unit_id),
                createAdRequest(), new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialAd = null;
                    }
                });
    }

    @Override
    public void showRewarded(OnRewardListener listener) {
        if (rewardedAd != null) {
            rewardedAd.show(activity, rewardItem -> listener.onReward(rewardItem.getAmount()));
            rewardedAd = null;
            loadRewarded();
        } else {
            Log.d(TAG, "Rewarded ad not ready");
            loadRewarded();
        }
    }

    private void loadRewarded() {
        RewardedAd.load(activity, activity.getString(R.string.rewarded_ad_unit_id),
                createAdRequest(), new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                    }
                });
    }

    private AdRequest createAdRequest() {
        return new AdRequest.Builder().build();
    }

    @Override
    public void onResume() {
        if (bannerAdView != null) bannerAdView.resume();
    }

    @Override
    public void onPause() {
        if (bannerAdView != null) bannerAdView.pause();
    }

    @Override
    public void onDestroy() {
        if (bannerAdView != null) bannerAdView.destroy();
    }
}
