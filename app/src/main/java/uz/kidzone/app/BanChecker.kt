package uz.kidzone.app

import android.util.Log

/** Checks if a user is banned by reading users/{uid}.status from Firestore. */
object BanChecker {

    private const val TAG = "BanChecker"

    enum class Status { ACTIVE, BANNED, ERROR }

    fun interface Callback {
        fun onResult(status: Status)
    }

    /** Package-private — injectable DocProvider for unit testing. */
    fun interface DocProvider {
        fun getDoc(uid: String, cb: DocCallback)
    }

    /** Package-private — injectable DocCallback for unit testing. */
    interface DocCallback {
        fun onDoc(exists: Boolean, status: String?)
        fun onError() {}
    }

    /** Production entry point — reads from Firestore. */
    @JvmStatic
    fun checkAsync(uid: String?, sync: FirestoreSync, cb: Callback) {
        if (uid == null || !sync.isAvailable()) {
            cb.onResult(Status.ERROR)
            return
        }
        checkAsync(uid, { u, docCb ->
            sync.getDb()!!.collection("users").document(u).get()
                .addOnSuccessListener { snap ->
                    val status = if (snap.exists()) snap.getString("status") else null
                    docCb.onDoc(snap.exists(), status)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "ban check failed: ${e.message}")
                    docCb.onError()
                }
        }, cb)
    }

    /**
     * Package-private — injectable DocProvider for unit testing.
     * @JvmStatic makes this callable as BanChecker.checkAsync(uid, provider, cb) from Java.
     */
    @JvmStatic
    fun checkAsync(uid: String?, provider: DocProvider, cb: Callback) {
        if (uid == null) {
            cb.onResult(Status.ERROR)
            return
        }
        provider.getDoc(uid, object : DocCallback {
            override fun onDoc(exists: Boolean, status: String?) {
                when {
                    !exists || status == null -> cb.onResult(Status.ACTIVE)
                    status == "banned" -> cb.onResult(Status.BANNED)
                    else -> cb.onResult(Status.ACTIVE)
                }
            }

            override fun onError() {
                cb.onResult(Status.ERROR)
            }
        })
    }
}
