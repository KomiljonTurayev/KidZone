package uz.kidzone.app.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class TelegramLoginStart(val loginId: String, val botUrl: String)

sealed interface TelegramLoginStatus {
    data object Pending : TelegramLoginStatus
    data object Expired : TelegramLoginStatus
    data class Confirmed(val customToken: String, val displayName: String?) : TelegramLoginStatus
}

/** The backend's Telegram login endpoints (start, status), authenticated with the guest's ID token. */
interface TelegramAuthApi {
    /** Best-effort wake-up of a sleeping backend; never fails. */
    suspend fun warmUp()
    suspend fun start(idToken: String): TelegramLoginStart
    suspend fun status(idToken: String, loginId: String): TelegramLoginStatus
}

class OkHttpTelegramAuthApi(
    private val baseUrl: String,
    sharedClient: OkHttpClient,
) : TelegramAuthApi {

    // The backend runs on a free Render instance that sleeps when idle and needs ~50s to
    // wake. OkHttp's 10s defaults made the first sign-in after a quiet period fail with
    // "check your internet"; these limits cover a cold start. newBuilder() shares the
    // connection pool with the app's client.
    private val httpClient = sharedClient.newBuilder()
        .connectTimeout(COLD_START_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(COLD_START_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(COLD_START_TIMEOUT_SECONDS + 10, TimeUnit.SECONDS)
        .build()

    override suspend fun warmUp() {
        withContext(Dispatchers.IO) {
            try {
                httpClient.newCall(Request.Builder().url("$baseUrl/health").get().build()).execute().close()
            } catch (_: IOException) {
                // Only a head start for the real request, which reports failures itself.
            }
        }
    }

    override suspend fun start(idToken: String): TelegramLoginStart =
        parseStart(send(request("/auth/telegram/start", idToken).post(ByteArray(0).toRequestBody()).build()))

    override suspend fun status(idToken: String, loginId: String): TelegramLoginStatus =
        parseStatus(send(request("/auth/telegram/status/$loginId", idToken).get().build()))

    private fun request(path: String, idToken: String) = Request.Builder()
        .url(baseUrl + path)
        .header("Authorization", "Bearer $idToken")

    private suspend fun send(request: Request): String = withContext(Dispatchers.IO) {
        try {
            httpClient.newCall(request).execute().use { response ->
                when (response.code) {
                    in 200..299 -> response.body?.string().orEmpty()
                    404 -> throw AuthException(AuthException.Reason.EXPIRED)
                    503 -> throw AuthException(AuthException.Reason.NOT_CONFIGURED)
                    else -> throw AuthException(AuthException.Reason.FAILED)
                }
            }
        } catch (e: IOException) {
            throw AuthException(AuthException.Reason.NETWORK, e)
        }
    }

    internal companion object {
        const val COLD_START_TIMEOUT_SECONDS = 60L

        fun parseStart(json: String): TelegramLoginStart = parse(json) {
            TelegramLoginStart(loginId = it.getString("loginId"), botUrl = it.getString("botUrl"))
        }

        fun parseStatus(json: String): TelegramLoginStatus = parse(json) {
            when (it.getString("status")) {
                "PENDING" -> TelegramLoginStatus.Pending
                "EXPIRED" -> TelegramLoginStatus.Expired
                "CONFIRMED" -> TelegramLoginStatus.Confirmed(
                    customToken = it.getString("customToken"),
                    displayName = it.optString("displayName").ifBlank { null },
                )
                else -> throw AuthException(AuthException.Reason.FAILED)
            }
        }

        private inline fun <T> parse(json: String, read: (JSONObject) -> T): T = try {
            read(JSONObject(json))
        } catch (e: JSONException) {
            throw AuthException(AuthException.Reason.FAILED, e)
        }
    }
}
