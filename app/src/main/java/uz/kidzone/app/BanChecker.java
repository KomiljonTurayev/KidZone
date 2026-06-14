package uz.kidzone.app;

import android.util.Log;

/** Checks if a user is banned by reading users/{uid}.status from Firestore. */
public final class BanChecker {

    private static final String TAG = "BanChecker";

    public enum Status { ACTIVE, BANNED, ERROR }

    public interface Callback {
        void onResult(Status status);
    }

    interface DocProvider {
        void getDoc(String uid, DocCallback cb);
    }

    interface DocCallback {
        void onDoc(boolean exists, String status);
        default void onError() {}
    }

    /** Production entry point — reads from Firestore. */
    public static void checkAsync(String uid, FirestoreSync sync, Callback cb) {
        if (uid == null || !sync.isAvailable()) {
            cb.onResult(Status.ERROR);
            return;
        }
        checkAsync(uid, (u, docCb) ->
            sync.getDb().collection("users").document(u).get()
                .addOnSuccessListener(snap -> {
                    String status = snap.exists() ? snap.getString("status") : null;
                    docCb.onDoc(snap.exists(), status);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "ban check failed: " + e.getMessage());
                    docCb.onError();
                }),
            cb
        );
    }

    /** Package-private — injectable DocProvider for unit testing. */
    static void checkAsync(String uid, DocProvider provider, Callback cb) {
        if (uid == null) {
            cb.onResult(Status.ERROR);
            return;
        }
        provider.getDoc(uid, new DocCallback() {
            @Override
            public void onDoc(boolean exists, String status) {
                if (!exists || status == null) {
                    cb.onResult(Status.ACTIVE);
                } else if ("banned".equals(status)) {
                    cb.onResult(Status.BANNED);
                } else {
                    cb.onResult(Status.ACTIVE);
                }
            }

            @Override
            public void onError() {
                cb.onResult(Status.ERROR);
            }
        });
    }
}
