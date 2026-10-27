package uz.kidzone.app.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import uz.kidzone.app.BackendConfig
import uz.kidzone.app.KidZoneApplication

/**
 * Wires the auth module once per process. Kept as a small service locator because the rest
 * of the app is built on singletons; everything it hands out is an interface, so classes
 * receive their collaborators through constructors and stay testable.
 */
object AuthGraph {

    @Volatile private var repository: AuthRepository? = null
    @Volatile private var googleSource: GoogleIdTokenSource? = null

    val auth: AuthRepository
        get() = repository ?: error("AuthGraph.init() must run in Application.onCreate()")

    /** Safe before [init] (and in unit tests): null. */
    val currentUid: String?
        get() = repository?.state?.value?.uid

    /** Safe before [init] (and in unit tests): no session, no token. */
    val idTokens: IdTokenProvider
        get() = repository ?: IdTokenProvider { null }

    val featureGate: FeatureGate
        get() = FeatureGate(repository?.state ?: MutableStateFlow(AccountState.Unavailable))

    val googleSignIn: GoogleIdTokenSource
        get() = googleSource ?: CredentialManagerGoogleSignIn(null)

    val telegramLogin: TelegramLoginFlow
        get() = TelegramLoginFlow(
            api = OkHttpTelegramAuthApi(BackendConfig.BASE_URL, KidZoneApplication.httpClient),
            auth = auth,
        )

    fun init(context: Context, appScope: CoroutineScope, listeners: List<AccountListener>) {
        if (repository != null) return
        val firebaseAuth = try {
            FirebaseAuth.getInstance()
        } catch (e: IllegalStateException) {
            null // built without google-services.json
        }
        val repo = FirebaseAuthRepository(firebaseAuth)
        repository = repo
        googleSource = CredentialManagerGoogleSignIn(CredentialManagerGoogleSignIn.webClientIdOf(context))
        AccountSessionCoordinator(repo.state, listeners).start(appScope)
    }
}
