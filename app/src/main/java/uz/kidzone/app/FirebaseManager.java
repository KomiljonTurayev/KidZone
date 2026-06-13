package uz.kidzone.app;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Thin wrapper around FirebaseAuth. Falls back to a "not available" state if the app
 * was built without google-services.json, so the app keeps working offline.
 */
public class FirebaseManager {

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String message);
    }

    private static FirebaseManager instance;

    private final FirebaseAuth auth;

    FirebaseManager(FirebaseAuth auth) {
        this.auth = auth;
    }

    public static synchronized FirebaseManager init(Context ctx) {
        if (instance == null) {
            FirebaseAuth auth;
            try {
                auth = FirebaseAuth.getInstance();
            } catch (IllegalStateException e) {
                auth = null;
            }
            instance = new FirebaseManager(auth);
        }
        return instance;
    }

    public static synchronized FirebaseManager getInstance() {
        return instance != null ? instance : new FirebaseManager(null);
    }

    public boolean isAvailable() {
        return auth != null;
    }

    public FirebaseUser getCurrentUser() {
        return auth != null ? auth.getCurrentUser() : null;
    }

    public String getUid() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void signInWithEmail(String email, String password, AuthCallback cb) {
        if (auth == null) {
            cb.onError("Firebase is not configured");
            return;
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> cb.onSuccess(result.getUser()))
            .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public void createAccountWithEmail(String email, String password, AuthCallback cb) {
        if (auth == null) {
            cb.onError("Firebase is not configured");
            return;
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> cb.onSuccess(result.getUser()))
            .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public void ensureAuthAsync(Runnable onReady) {
        if (auth == null) { onReady.run(); return; }
        if (auth.getCurrentUser() != null) { onReady.run(); return; }
        auth.signInAnonymously().addOnCompleteListener(task -> onReady.run());
    }

    public void signOut() {
        if (auth != null) auth.signOut();
    }
}
