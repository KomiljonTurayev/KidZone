package uz.kidzone.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * Thin wrapper around FirebaseAuth. Falls back to a "not available" state if the app
 * was built without google-services.json, so the app keeps working offline.
 *
 * Constructor is internal (not private) so tests in the same module can do:
 *   new FirebaseManager(null)  (Java) / FirebaseManager(null) (Kotlin)
 */
class FirebaseManager internal constructor(private val auth: FirebaseAuth?) {

    interface AuthCallback {
        fun onSuccess(user: FirebaseUser)
        fun onError(message: String)
    }

    companion object {
        @Volatile private var instance: FirebaseManager? = null

        @JvmStatic
        @Synchronized
        fun init(@Suppress("UNUSED_PARAMETER") ctx: Context): FirebaseManager {
            return instance ?: run {
                val auth = try { FirebaseAuth.getInstance() } catch (e: IllegalStateException) { null }
                FirebaseManager(auth).also { instance = it }
            }
        }

        @JvmStatic
        @Synchronized
        fun getInstance(): FirebaseManager = instance ?: FirebaseManager(null)
    }

    fun isAvailable(): Boolean = auth != null

    fun getCurrentUser(): FirebaseUser? = auth?.currentUser

    fun getUid(): String? = getCurrentUser()?.uid

    fun signInWithEmail(email: String, password: String, cb: AuthCallback) {
        if (auth == null) { cb.onError("Firebase is not configured"); return }
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { cb.onSuccess(it.user!!) }
            .addOnFailureListener { cb.onError(it.message ?: "Unknown error") }
    }

    fun createAccountWithEmail(email: String, password: String, cb: AuthCallback) {
        if (auth == null) { cb.onError("Firebase is not configured"); return }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { cb.onSuccess(it.user!!) }
            .addOnFailureListener { cb.onError(it.message ?: "Unknown error") }
    }

    fun ensureAuthAsync(onReady: Runnable) {
        if (auth == null) { onReady.run(); return }
        if (auth.currentUser != null) { onReady.run(); return }
        auth.signInAnonymously().addOnCompleteListener { onReady.run() }
    }

    fun signOut() { auth?.signOut() }
}
