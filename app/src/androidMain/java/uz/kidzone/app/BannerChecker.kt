package uz.kidzone.app

import android.util.Log

/** Reads config/banner from Firestore; returns BannerData if active, null otherwise. */
object BannerChecker {

    private const val TAG = "BannerChecker"

    data class BannerData(
        @JvmField val title: String,
        @JvmField val body: String,
        @JvmField val url: String
    )

    fun interface Callback {
        fun onResult(banner: BannerData?)
    }

    /** Package-private — injectable DocCallback for unit testing. */
    interface DocCallback {
        fun onDoc(exists: Boolean, active: Boolean, title: String?, body: String?, url: String?)
        fun onError() {}
    }

    /**
     * Package-private DocProvider — injectable for unit testing.
     * Declared as a functional interface so Java tests can implement it with a lambda:
     *   cb -> cb.onDoc(...)
     */
    fun interface DocProvider {
        fun provide(cb: DocCallback)
    }

    /** Production entry point — tries backend first, falls back to Firestore config/banner. */
    @JvmStatic
    fun checkAsync(sync: FirestoreSync, callback: Callback) {
        BackendClient.fetchFirstActiveBanner { backendBanner ->
            if (backendBanner != null) {
                callback.onResult(backendBanner)
            } else {
                // Fallback: read from Firestore
                if (!sync.isAvailable()) { callback.onResult(null); return@fetchFirstActiveBanner }
                checkAsync(DocProvider { docCb ->
                    sync.getDb()!!.collection("config").document("banner").get()
                        .addOnSuccessListener { snap ->
                            docCb.onDoc(
                                snap.exists(),
                                snap.getBoolean("active") == true,
                                snap.getString("title"),
                                snap.getString("body"),
                                snap.getString("url")
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "banner check failed: $e")
                            docCb.onError()
                        }
                }, callback)
            }
        }
    }

    /**
     * Package-private — injectable DocProvider for unit testing.
     * @JvmStatic makes this callable as BannerChecker.checkAsync(provider, callback) from Java.
     */
    @JvmStatic
    fun checkAsync(provider: DocProvider, callback: Callback) {
        provider.provide(object : DocCallback {
            override fun onDoc(exists: Boolean, active: Boolean, title: String?, body: String?, url: String?) {
                if (!exists || !active || url.isNullOrEmpty()) {
                    callback.onResult(null)
                } else {
                    callback.onResult(BannerData(
                        title = title ?: "",
                        body = body ?: "",
                        url = url
                    ))
                }
            }

            override fun onError() {
                callback.onResult(null)
            }
        })
    }
}
