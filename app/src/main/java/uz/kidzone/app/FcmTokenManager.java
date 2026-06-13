package uz.kidzone.app;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;

/** Registers the device FCM token to Firestore so the admin backend can push notifications. */
public final class FcmTokenManager {

    private FcmTokenManager() {}

    interface TokenProvider {
        void getToken(TokenCallback cb);
    }

    interface TokenCallback {
        void onToken(String token);
    }

    /** Production entry point — uses real FirebaseMessaging. */
    public static void registerToken(String uid, FirestoreSync sync) {
        registerToken(uid, sync, cb ->
            FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(cb::onToken)
                .addOnFailureListener(e -> {
                    Log.w("FcmTokenManager", "getToken failed: " + e.getMessage());
                    cb.onToken(null);
                })
        );
    }

    /** Package-private overload with injectable TokenProvider for unit testing. */
    static void registerToken(String uid, FirestoreSync sync, TokenProvider provider) {
        if (uid == null || !sync.isAvailable()) return;
        provider.getToken(token -> {
            if (token != null) sync.updateFcmToken(uid, token);
        });
    }
}
