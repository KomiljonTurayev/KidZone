package uz.kidzone.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import uz.kidzone.app.auth.AuthGraph
import java.util.concurrent.TimeUnit

/** The caller's family (Faza 16): whose profiles they use, their role and the members. */
data class Family(val ownerUid: String, val isOwner: Boolean, val members: List<FamilyMemberInfo>)

data class FamilyMemberInfo(val uid: String, val joinedAt: String)

data class FamilyInvite(val code: String, val expiresAt: String)

/** Why a family call failed; the screen turns it into text in the app language. */
enum class FamilyError { NOT_FOUND, FORBIDDEN, TOO_MANY_ATTEMPTS, NETWORK }

sealed interface FamilyResult<out T> {
    data class Ok<T>(val value: T) : FamilyResult<T>
    data class Failed(val error: FamilyError) : FamilyResult<Nothing>
}

/** `/family` endpoints of the backend. Membership itself is written only by the backend. */
object FamilyApiClient {

    private const val TAG = "FamilyApiClient"
    private val BASE = "${BackendConfig.BASE_URL}/family"

    // A sleeping free-tier backend can take a while to wake up; the parent is waiting on a
    // spinner, so allow more than the heartbeat does.
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    suspend fun family(): FamilyResult<Family> = call("GET", BASE) { parseFamily(it) }

    suspend fun invite(): FamilyResult<FamilyInvite> = call("POST", "$BASE/invite") {
        val root = JSONObject(it)
        FamilyInvite(root.getString("code"), root.getString("expiresAt"))
    }

    suspend fun join(code: String): FamilyResult<Family> =
        call("POST", "$BASE/join", JSONObject().put("code", code).toString()) { parseFamily(it) }

    suspend fun leave(): FamilyResult<Unit> = call("POST", "$BASE/leave") { }

    suspend fun removeMember(uid: String): FamilyResult<Unit> = call("DELETE", "$BASE/members/$uid") { }

    private suspend fun <T> call(
        method: String,
        url: String,
        json: String? = null,
        parse: (String) -> T,
    ): FamilyResult<T> = withContext(Dispatchers.IO) {
        try {
            val idToken = AuthGraph.idTokens.idToken() ?: return@withContext FamilyResult.Failed(FamilyError.NETWORK)
            val body = (json ?: if (method == "POST") "{}" else null)
                ?.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $idToken")
                .method(method, body)
                .build()
            httpClient.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> FamilyResult.Ok(parse(response.body?.string().orEmpty()))
                    else -> FamilyResult.Failed(errorFor(response.code))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "$method $url failed: ${e.message}")
            FamilyResult.Failed(FamilyError.NETWORK)
        }
    }
}

internal fun errorFor(httpCode: Int): FamilyError = when (httpCode) {
    400, 404 -> FamilyError.NOT_FOUND
    401, 403 -> FamilyError.FORBIDDEN
    429 -> FamilyError.TOO_MANY_ATTEMPTS
    else -> FamilyError.NETWORK
}

internal fun parseFamily(json: String): Family {
    val root = JSONObject(json)
    val members = root.optJSONArray("members")
    return Family(
        ownerUid = root.getString("ownerUid"),
        isOwner = root.optString("role") == "owner",
        members = (0 until (members?.length() ?: 0)).map { i ->
            val m = members!!.getJSONObject(i)
            FamilyMemberInfo(m.getString("uid"), m.optString("joinedAt"))
        },
    )
}
