package uz.kidzone.app

import android.view.ViewGroup

/**
 * Interface for managing advertisements in the application.
 * Follows the Interface Segregation and Dependency Inversion principles.
 */
interface IAdsManager {
    fun initialize()

    fun loadBanner(container: ViewGroup, isTablet: Boolean)

    fun showInterstitial()

    fun showRewarded(listener: OnRewardListener)

    fun onResume()

    fun onPause()

    fun onDestroy()

    /**
     * Listener for rewarded ad events.
     * fun interface enables SAM conversion from Java lambdas.
     */
    fun interface OnRewardListener {
        fun onReward(amount: Int)
    }

    /**
     * Listener for banner visibility and height updates.
     */
    interface BannerListener {
        fun onBannerLoaded(heightDp: Int)
        fun onBannerFailed()
    }
}
