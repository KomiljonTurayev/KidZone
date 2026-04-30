package uz.kidzone.app;

import android.view.ViewGroup;

/**
 * Interface for managing advertisements in the application.
 * Follows the Interface Segregation and Dependency Inversion principles.
 */
public interface IAdsManager {
    void initialize();
    
    void loadBanner(ViewGroup container, boolean isTablet);
    
    void showInterstitial();
    
    void showRewarded(OnRewardListener listener);
    
    void onResume();
    
    void onPause();
    
    void onDestroy();

    /**
     * Listener for rewarded ad events.
     */
    interface OnRewardListener {
        void onReward(int amount);
    }

    /**
     * Listener for banner visibility and height updates.
     */
    interface BannerListener {
        void onBannerLoaded(int heightDp);
        void onBannerFailed();
    }
}
