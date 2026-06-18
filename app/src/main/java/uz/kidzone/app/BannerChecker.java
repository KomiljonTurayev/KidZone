package uz.kidzone.app;

import android.util.Log;
import java.util.function.Consumer;

/** Reads config/banner from Firestore; returns BannerData if active, null otherwise. */
public final class BannerChecker {

    private static final String TAG = "BannerChecker";

    public static class BannerData {
        public final String title;
        public final String body;
        public final String url;
        public BannerData(String title, String body, String url) {
            this.title = title; this.body = body; this.url = url;
        }
    }

    public interface Callback {
        void onResult(BannerData banner);
    }

    interface DocCallback {
        void onDoc(boolean exists, boolean active, String title, String body, String url);
        default void onError() {}
    }

    /** Production entry point — reads config/banner from Firestore. */
    public static void checkAsync(FirestoreSync sync, Callback callback) {
        if (!sync.isAvailable()) { callback.onResult(null); return; }
        checkAsync(docCb ->
            sync.getDb().collection("config").document("banner").get()
                .addOnSuccessListener(snap ->
                    docCb.onDoc(
                        snap.exists(),
                        Boolean.TRUE.equals(snap.getBoolean("active")),
                        snap.getString("title"),
                        snap.getString("body"),
                        snap.getString("url")
                    )
                )
                .addOnFailureListener(e -> {
                    Log.w(TAG, "banner check failed: " + e);
                    docCb.onError();
                }),
            callback
        );
    }

    /** Package-private — injectable Consumer<DocCallback> for unit testing. */
    static void checkAsync(Consumer<DocCallback> provider, Callback callback) {
        provider.accept(new DocCallback() {
            @Override
            public void onDoc(boolean exists, boolean active,
                              String title, String body, String url) {
                if (!exists || !active || url == null || url.isEmpty()) {
                    callback.onResult(null);
                } else {
                    callback.onResult(new BannerData(
                        title != null ? title : "",
                        body != null ? body : "",
                        url
                    ));
                }
            }

            @Override
            public void onError() {
                callback.onResult(null);
            }
        });
    }
}
