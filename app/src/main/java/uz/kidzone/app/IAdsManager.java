package uz.kidzone.app;

import android.view.ViewGroup;

public interface IAdsManager {
    void loadBanner(ViewGroup container, boolean isTablet);
    void showInterstitial();
    void showRewarded(OnRewardListener listener);
    void onResume();
    void onPause();
    void onDestroy();

    interface OnRewardListener {
        void onReward(int amount);
    }
}
