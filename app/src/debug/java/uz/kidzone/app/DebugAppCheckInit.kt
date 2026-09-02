package uz.kidzone.app

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug-build-only: installs the App Check debug provider so local/emulator
 * builds can call the (App Check-enforced) Firebase AI Logic backend without
 * Play Integrity. On first run, logcat (tag "DebugAppCheckProvider") prints a
 * debug token that must be added in Firebase Console -> App Check -> this
 * app -> Manage debug tokens.
 */
object DebugAppCheckInit {
    fun install() {
        try {
            Firebase.appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance(),
            )
        } catch (e: Exception) {
            android.util.Log.w("DebugAppCheckInit", "Firebase AppCheck not initialized: ${e.message}")
        }
    }
}
