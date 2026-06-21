package uz.kidzone.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

/** Registers the device FCM token to Firestore so the admin backend can push notifications. */
object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    /** Package-private — accessible from tests in same package without name mangling. */
    fun interface TokenProvider {
        fun getToken(cb: TokenCallback)
    }

    /** Package-private — accessible from tests in same package without name mangling. */
    fun interface TokenCallback {
        fun onToken(token: String?)
    }

    /** Production entry point — uses real FirebaseMessaging. */
    @JvmStatic
    fun registerToken(uid: String?, sync: FirestoreSync) {
        registerToken(uid, sync) { cb ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { cb.onToken(it) }
                .addOnFailureListener { e ->
                    Log.w(TAG, "getToken failed: ${e.message}")
                    cb.onToken(null)
                }
        }
    }

    /**
     * Package-private overload with injectable TokenProvider for unit testing.
     * @JvmStatic makes this callable as FcmTokenManager.registerToken(uid, sync, provider) from Java.
     */
    @JvmStatic
    fun registerToken(uid: String?, sync: FirestoreSync, provider: TokenProvider) {
        if (uid == null || !sync.isAvailable()) return
        FirebaseMessaging.getInstance().subscribeToTopic("all-users")
            .addOnFailureListener { e -> Log.w(TAG, "topic sub failed: $e") }
        provider.getToken { token ->
            if (token != null) sync.updateFcmToken(uid, token)
        }
    }
}
