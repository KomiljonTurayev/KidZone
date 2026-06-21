package uz.kidzone.app

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Implementation of IAdsManager using Google Mobile Ads (AdMob).
 * Adheres to SRP by focusing only on Advertisement lifecycle.
 */
class AdsManager(private val activity: Activity) : IAdsManager {

    companion object {
        private const val TAG = "AdsManager"
        private const val BANNER_RETRY_DELAY_MS = 30_000L
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var bannerListener: IAdsManager.BannerListener? = null
    private var bannerAdView: AdView? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    override fun initialize() {
        configureAdMob()
        MobileAds.initialize(activity) {
            Log.d(TAG, "AdMob Initialized")
            loadInterstitial()
            loadRewarded()
        }
    }

    private fun configureAdMob() {
        val config = RequestConfiguration.Builder()
            // COPPA compliance: Tag for child-directed treatment
            .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
            // Google Play Families Policy: Max ad content rating (G = General)
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            // GDPR/Underage: Tag for under age of consent
            .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
            .build()
        MobileAds.setRequestConfiguration(config)
    }

    override fun loadBanner(container: ViewGroup, isTablet: Boolean) {
        loadBanner(container, isTablet, null)
    }

    fun loadBanner(container: ViewGroup, isTablet: Boolean, listener: IAdsManager.BannerListener?) {
        cleanupBanner(container)
        bannerListener = listener

        val adSize = if (isTablet) AdSize.LEADERBOARD else AdSize.BANNER
        bannerAdView = AdView(activity).also { adView ->
            adView.setAdSize(adSize)
            adView.adUnitId = activity.getString(R.string.banner_ad_unit_id)
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    container.visibility = View.VISIBLE
                    bannerListener?.onBannerLoaded(adSize.height)
                }

                override fun onAdFailedToLoad(e: LoadAdError) {
                    Log.e(TAG, "Banner failed: ${e.message}")
                    bannerListener?.onBannerFailed()
                    mainHandler.postDelayed(
                        { loadBanner(container, isTablet) },
                        BANNER_RETRY_DELAY_MS
                    )
                }
            }
            container.addView(adView)
            adView.loadAd(createAdRequest())
        }
    }

    private fun cleanupBanner(container: ViewGroup) {
        bannerAdView?.let {
            container.removeView(it)
            it.destroy()
            bannerAdView = null
        }
    }

    override fun showInterstitial() {
        if (interstitialAd != null) {
            interstitialAd!!.show(activity)
            interstitialAd = null
            loadInterstitial()
        } else {
            Log.d(TAG, "Interstitial not ready yet")
            loadInterstitial()
        }
    }

    private fun loadInterstitial() {
        InterstitialAd.load(
            activity,
            activity.getString(R.string.interstitial_ad_unit_id),
            createAdRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(e: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    override fun showRewarded(listener: IAdsManager.OnRewardListener) {
        if (rewardedAd != null) {
            rewardedAd!!.show(activity) { rewardItem -> listener.onReward(rewardItem.amount) }
            rewardedAd = null
            loadRewarded()
        } else {
            Log.d(TAG, "Rewarded ad not ready")
            loadRewarded()
        }
    }

    private fun loadRewarded() {
        RewardedAd.load(
            activity,
            activity.getString(R.string.rewarded_ad_unit_id),
            createAdRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(e: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    private fun createAdRequest(): AdRequest {
        // Force Non-Personalized Ads (NPA) for all requests in kids' app
        val extras = Bundle().apply { putString("npa", "1") }
        return AdRequest.Builder()
            .addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter::class.java, extras)
            .build()
    }

    override fun onResume() {
        bannerAdView?.resume()
    }

    override fun onPause() {
        bannerAdView?.pause()
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
    }
}
