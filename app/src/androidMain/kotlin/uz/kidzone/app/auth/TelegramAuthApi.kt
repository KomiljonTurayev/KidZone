package uz.kidzone.app.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

data class TelegramLoginStart(val loginId: String, val botUrl: String)

sealed interface TelegramLoginStatus {
    data object Pending : TelegramLoginStatus
    data object Expired : TelegramLoginStatus
    data class Confirmed(val customToken: String, val displayName: String?) : TelegramLoginStatus
}

/** The backend's Telegram login endpoints (start, status), authenticated with the guest's ID token. */
interface TelegramAuthApi {
    suspend fun start(idToken: String): TelegramLoginStart
    suspend fun status(idToken: String, loginId: String): TelegramLoginStatus
}

class OkHttpTelegramAuthApi(
    private val baseUrl: String,
    private val httpClient: OkHttpClient,
) : TelegramAuthApi {

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
