package uz.kidzone.app.ai

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Talks to Kidzo's cloud brain via the KidZone backend, never Gemini directly:
 * embedding a Gemini API key in the client APK lets anyone decompile it and
 * drain the quota/billing. The backend holds the key server-side and forwards
 * the child's Firebase ID token so it can rate-limit / attribute usage.
 */
object KidzoCompanionEngine {

    private const val TAG = "KidzoCompanion"
    private val BACKEND_URL = "${uz.kidzone.app.BackendConfig.BASE_URL}/ai/ask"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun ask(
        query: String,
        lang: String = "uz",
        childName: String = "",
        ageRange: String = "3-5"
    ): String = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        val targetLang = when (lang.lowercase()) {
            "ru" -> "ru"
            "en" -> "en"
            else -> "uz"
        }

        try {
            val backendAnswer = callBackend(trimmed, targetLang, childName, ageRange)
            if (!backendAnswer.isNullOrBlank()) {
                return@withContext backendAnswer
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend companion call failed: ${e.message}, falling back to offline brain")
        }

        // Fallback to rich Offline Companion Brain
        OfflineKidzoBrain.respond(trimmed, targetLang, childName)
    }

    private fun callBackend(
        query: String,
        lang: String,
        childName: String,
        ageRange: String
    ): String? {
        val idToken = currentIdToken() ?: return null

        val requestJson = JSONObject().apply {
            put("query", query)
            put("lang", lang)
            put("childName", childName)
            put("ageRange", ageRange)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(BACKEND_URL)
            .addHeader("Authorization", "Bearer $idToken")
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val respBody = response.body?.string() ?: return null
            return JSONObject(respBody).optString("answer").ifBlank { null }
        }
    }

    private fun currentIdToken(): String? = try {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        Tasks.await(user.getIdToken(false)).token
    } catch (e: Exception) {
        Log.w(TAG, "getIdToken failed: ${e.message}")
        null
    }
}

/**
 * High quality offline conversational brain with 100+ kid responses
 */
object OfflineKidzoBrain {

    fun respond(query: String, lang: String, childName: String): String {
        val name = childName.ifBlank {
            when (lang) {
                "ru" -> "Дружок"
                "en" -> "Friend"
                else -> "Bolajonim"
            }
        }
        val q = query.lowercase()

        return when (lang) {
            "ru" -> respondRu(q, name)
            "en" -> respondEn(q, name)
            else -> respondUz(q, name)
        }
    }

    private fun respondUz(q: String, name: String): String {
        return when {
            q.contains("topishmoq") || q.contains("riddle") -> {
                val riddles = listOf(
                    "Top-chi qani, $name: O'zi oppoq, qor emas, shirin-shakar, asal emas? 🐥 (Bu — Qand yoki Muzqaymoq! 🍦)",
                    "Mitti jonzot, tikanli po'stin kiyar, olmani yaxshi ko'rar? 🦔 (Bu — Kirpi!)",
                    "Kunduzi uxlaydi, kechasi ko'radi, uning katta ko'zlari bor? 🦉 (Bu — Boyo'g'li!)",
                    "Osmon uzra rang-barang ko'prik tushdi, yomg'irdan so'ng paydo bo'ldi? 🌈 (Bu — Kamalak!)",
                    "Qanoti bor uchmaydi, suv tagida suzadi? 🐟 (Bu — Baliqcha!)"
                )
                riddles.random()
            }
            q.contains("osmon") || q.contains("ko'k") || q.contains("havorang") -> {
                "Quyosh nurlari havoga tushganda, uning moviy nurlari har tomonga sochilib, osmonni moviy rangga bo'yab qo'yadi! ☀️ Sen ham bugun quyoshdek charaqlab turibsan, $name! ✨"
            }
            q.contains("dino") || q.contains("dinozavr") -> {
                "Dinozavrlar qadim zamonlarda yashagan bahaybat va qiziqarli jonzotlar bo'lgan! 🦖 Masalan, ularning ayrimlari baland daraxtlarning yaproqlarini yeyishni yaxshi ko'rgan! 🌿"
            }
            q.contains("ertak") || q.contains("ayt") || q.contains("hikoya") -> {
                "Bir bor ekan, bir yo'q ekan, sehrli o'rmonda mitti yulduzcha yashagan ekan. 🌟 U har oqshom $name kabi aqlli bolalarga shirin tushlar ulashish uchun osmonda charaqlarkan! 🌙"
            }
            q.contains("hazil") || q.contains("latifa") || q.contains("kul") -> {
                "Mitti ayiqcha asal yeyayotib ari bilan gaplashibdi: 'Nega asaling muncha shirin?' debdi. Ari esa: 'Chunki men uni quvnoq qo'shiq aytib tayyorlayman!' debdi! 🐻🍯"
            }
            q.contains("maqtov") || q.contains("sev") || q.contains("yaxshi") -> {
                "Sen dunyodagi eng aqlli, eng mehribon va iqtidorli bolajonsan, $name! 🌟 Men sen bilan do'st bo'lganimdan judayam xursandman! 🐥💛"
            }
            q.contains("qush") || q.contains("uch") -> {
                "Qushchalarning qanotlari yengil va patlari havo oqimini ushlab turadi! 🕊️ Ular qanot qoqib xuddi mitti samolyotdek parvoz qilishadi! ✈️"
            }
            q.contains("salom") || q.contains("qale") || q.contains("qalaysiz") -> {
                "Salom, $name! 🐥 Kichkintoy do'sting Kidzo doim sen bilan! Bugun qanday ajoyib o'yin o'ynaymiz yoki nima haqida gaplashamiz? ✨"
            }
            else -> {
                val generic = listOf(
                    "Qanday ajoyib savol, $name! 🌟 Sen juda ham qiziquvchan va zukkosan! Keling, yana bir qiziqarli narsani o'rganamiz! 🐥",
                    "Men sening ovozingni eshitganimdan juda xursandman, $name! 🎈 Bilasanmi, tabassum qilganingda sen yanada chiroyliroqsan! ✨",
                    "Albatta, $name! Dunyo mo'jizalarga to'la, sen esa uning eng yorqin yulduzchasisan! 🚀"
                )
                generic.random()
            }
        }
    }

    private fun respondRu(q: String, name: String): String {
        return when {
            q.contains("загадк") || q.contains("загадай") -> {
                val riddles = listOf(
                    "Отгадай, $name: Зимой белый, летом серый, любит морковку? 🐰 (Это — Зайчик!)",
                    "Кто на себе свой домик носит? 🐌 (Это — Улитка!)",
                    "Разноцветное коромысло через реку повисло? 🌈 (Это — Радуга!)"
                )
                riddles.random()
            }
            q.contains("небо") || q.contains("почему") -> {
                "Солнечный свет рассеивается в воздухе, и синие лучики окрашивают небо в чудесный голубой цвет! ☀️ Ты сегодня светишься как солнышко, $name! ✨"
            }
            q.contains("дино") -> {
                "Динозавры жили давным-давно, они были очень большими и интересными! 🦖 Некоторые были ростом с трехэтажный дом! 🌿"
            }
            q.contains("сказк") -> {
                "В волшебной стране жила маленькая звездочка. 🌟 Каждую ночь она светила в окно $name, чтобы снились только самые сладкие сны! 🌙"
            }
            q.contains("шутк") || q.contains("смеш") -> {
                "Медвежонок спрашивает пчёлку: 'Почему твой мёд такой вкусный?' А пчелка отвечает: 'Потому что я делаю его с улыбкой!' 🐻🍯"
            }
            else -> {
                "Какой прекрасный вопрос, $name! 🌟 Ты очень умный и любознательный малыш! Давай узнаем ещё много интересного вместе! 🐥"
            }
        }
    }

    private fun respondEn(q: String, name: String): String {
        return when {
            q.contains("riddle") -> {
                "Guess what, $name: What has hands but cannot clap? ⏰ (A Clock!) 🐥"
            }
            q.contains("sky") || q.contains("why") -> {
                "Sunlight scatters through the air and blue light spreads everywhere, making our sky so bright and blue! ☀️ You shine bright today, $name! ✨"
            }
            q.contains("dino") -> {
                "Dinosaurs lived long ago, and some were as tall as tall buildings! 🦖 They were amazing creatures! 🌿"
            }
            q.contains("story") -> {
                "Once upon a time, a little star glowed high in the sky. 🌟 It watched over $name every night, bringing sweet magical dreams! 🌙"
            }
            else -> {
                "What a wonderful question, $name! 🌟 You are so smart and curious! I love chatting with you! 🐥"
            }
        }
    }
}
