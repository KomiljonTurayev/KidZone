package uz.kidzone.app.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Obtains a Google ID token from the user, to be exchanged for a Firebase session. */
fun interface GoogleIdTokenSource {
    suspend fun requestIdToken(activity: Activity): String
}

/** Google sign-in through Android's Credential Manager (the "Sign in with Google" sheet). */
class CredentialManagerGoogleSignIn(private val webClientId: String?) : GoogleIdTokenSource {

    override suspend fun requestIdToken(activity: Activity): String {
        val clientId = webClientId ?: throw AuthException(AuthException.Reason.NOT_CONFIGURED)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
            .build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw AuthException(AuthException.Reason.CANCELLED, e)
        } catch (e: NoCredentialException) {
            throw AuthException(AuthException.Reason.CANCELLED, e)
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Google sign-in failed: ${e.type}: ${e.message}")
            throw AuthException(AuthException.Reason.FAILED, e)
        }
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        throw AuthException(AuthException.Reason.FAILED)
    }

    companion object {
        private const val TAG = "GoogleSignIn"

        /**
         * The OAuth web client id the google-services plugin generates from google-services.json.
         * Looked up by name because the resource only exists once Google sign-in is enabled in
         * the Firebase console; without it Google sign-in reports NOT_CONFIGURED.
         */
        fun webClientIdOf(context: Context): String? {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            return if (resId == 0) null else context.getString(resId).takeIf { it.isNotBlank() }
        }
    }
}
