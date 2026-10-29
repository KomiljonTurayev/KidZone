package uz.kidzone.app.ui

import android.app.Activity
import java.lang.ref.WeakReference
import uz.kidzone.app.ui.viewmodel.DailyChallengeViewModel

internal class ChallengeBridge(
    private val viewModel: DailyChallengeViewModel,
    activity: Activity,
) {
    private val activity = WeakReference(activity)

    private fun onMain(block: () -> Unit) {
        activity.get()?.runOnUiThread(block)
    }

    @android.webkit.JavascriptInterface
    fun onGamesLoaded(json: String) {
        onMain { viewModel.updateGamesList(json) }
    }

    @android.webkit.JavascriptInterface
    fun onGameOpened(@Suppress("UNUSED_PARAMETER") gameId: String) {
    }

    /** Stars the child just earned in the WebView (games, badges, stickers). */
    @android.webkit.JavascriptInterface
    fun onStarsEarned(stars: Int) {
        if (stars in 1..1000) onMain { viewModel.onStarsEarned(stars) }
    }

    @android.webkit.JavascriptInterface
    fun onGameClosed(gameId: String) {
        onMain { viewModel.onGameClosed(gameId) }
    }
}
