package uz.kidzone.app;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

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

public class AdsManager {
    private static final String TAG = "AdsManager";
    private final Activity activity;
    
    private AdView bannerAdView;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;

    public AdsManager(Activity activity) {
        this.activity = activity;
        initAdMob();
    }

    private void initAdMob() {
        RequestConfiguration config = new RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build();
        MobileAds.setRequestConfiguration(config);
        MobileAds.initialize(activity, status -> {
            loadInterstitial();
            loadRewarded();
        });
    }

    public void loadBanner(ViewGroup container, boolean isTablet) {
        if (bannerAdView != null) {
            container.removeView(bannerAdView);
            bannerAdView.destroy();
        }

        bannerAdView = new AdView(activity);
        bannerAdView.setAdSize(isTablet ? AdSize.LEADERBOARD : AdSize.BANNER);
        bannerAdView.setAdUnitId(activity.getString(R.string.banner_ad_unit_id));

        bannerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                container.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError e) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> loadBanner(container, isTablet), 30000);
            }
        });

        container.addView(bannerAdView);
        bannerAdView.loadAd(new AdRequest.Builder().build());
    }

    public void showInterstitial() {
        if (interstitialAd != null) {
            interstitialAd.show(activity);
            interstitialAd = null;
            loadInterstitial();
        } else {
            loadInterstitial();
        }
    }

    private void loadInterstitial() {
        InterstitialAd.load(activity, activity.getString(R.string.interstitial_ad_unit_id),
                new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                    }
                });
    }

    public void showRewarded(OnRewardListener listener) {
        if (rewardedAd != null) {
            rewardedAd.show(activity, rewardItem -> listener.onReward(rewardItem.getAmount()));
            rewardedAd = null;
            loadRewarded();
        } else {
            loadRewarded();
        }
    }

    private void loadRewarded() {
        RewardedAd.load(activity, activity.getString(R.string.rewarded_ad_unit_id),
                new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                    }
                });
    }

    public void onResume() { if (bannerAdView != null) bannerAdView.resume(); }
    public void onPause() { if (bannerAdView != null) bannerAdView.pause(); }
    public void onDestroy() { if (bannerAdView != null) bannerAdView.destroy(); }

    public interface OnRewardListener {
        void onReward(int amount);
    }
}
