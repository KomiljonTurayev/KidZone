package uz.kidzone.app.kidzo

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class KidzoAgent private constructor(
    private val gemini: GeminiCaller,
    private val contentFilter: ContentFilter,
    private val mainThread: (() -> Unit) -> Unit,
) {
    private val _state = MutableStateFlow(KidzoState.IDLE)
    val state: StateFlow<KidzoState> = _state

    private val _cards = MutableStateFlow<List<ContentCard>>(emptyList())
    val cards: StateFlow<List<ContentCard>> = _cards

    private val _chatMessages = MutableStateFlow<List<String>>(emptyList())
    val chatMessages: StateFlow<List<String>> = _chatMessages

    private var onContentOpen: ((String) -> Unit)? = null

    @Volatile
    private var callGeneration: Int = 0

    companion object {
        /** Production factory — wires RealGeminiCaller. */
        @JvmStatic
        fun create(filter: ContentFilter, mainThread: (() -> Unit) -> Unit, @Suppress("UNUSED_PARAMETER") context: Context): KidzoAgent {
            return KidzoAgent(RealGeminiCaller(), filter, mainThread)
        }

        /** Static factory — no Gemini API needed; shows content.json top-5 directly. */
        @JvmStatic
        fun createStatic(filter: ContentFilter, mainThread: (() -> Unit) -> Unit): KidzoAgent {
            val staticCaller = GeminiCaller { prompt, onSuccess, _ ->
                Thread {
                    try { Thread.sleep(500) } catch (_: InterruptedException) {}
                    if (prompt.startsWith("Sen KidZone")) {
                        onSuccess("")   // triggers top-5 fallback in requestRecommendations()
                    } else {
                        onSuccess("Salom! 🐥 Bugun qaysi ertakni tinglaysiz?")
                    }
                }.start()
            }
            return KidzoAgent(staticCaller, filter, mainThread)
        }
    }

    fun setOnContentOpen(cb: (String) -> Unit) {
        onContentOpen = cb
    }

    fun getCurrentState(): KidzoState = _state.value

    /** FAB pressed: IDLE → THINKING → RECOMMENDATIONS */
    fun requestRecommendations() {
        val gen = ++callGeneration
        setState(KidzoState.THINKING, null)

        val top5 = contentFilter.getTop5()
        val promptBlock = contentFilter.toPromptBlock(top5)
        val prompt = buildRecommendationPrompt("Bolam", null, promptBlock)

        // Build id→item map for emoji lookup
        val itemMap = top5.associateBy { it.id }

        gemini.call(
            prompt,
            onResult = { text ->
                if (gen != callGeneration) return@call
                val parsed = ActionParser.parseRecommendations(text)
                var cardList = enrich(parsed, itemMap)
                if (cardList.isEmpty()) {
                    cardList = top5.map { item ->
                        ContentCard(item.id, item.titleUz, item.emoji, typeOf(item.id))
                    }
                }
                setState(KidzoState.RECOMMENDATIONS, cardList)
            },
            onError = { errorMsg ->
                if (gen != callGeneration) return@call
                setState(KidzoState.ERROR, errorMsg)
            }
        )
    }

    /** Open content via card or chat. */
    fun openContent(contentId: String) {
        mainThread { onContentOpen?.invoke(contentId) }
    }

    fun startChat() {
        setState(KidzoState.CHATTING, "Salom! Men Kidzo. Nima haqida gaplashamiz? 🐥")
    }

    fun sendChatMessage(userMessage: String) {
        val gen = ++callGeneration
        setState(KidzoState.THINKING, null)
        Thread {
            try { Thread.sleep(400) } catch (_: InterruptedException) {}
            if (gen != callGeneration) return@Thread
            setState(KidzoState.CHATTING, buildOfflineResponse(userMessage.lowercase().trim()))
        }.start()
    }

    fun dismiss() {
        callGeneration++
        setState(KidzoState.IDLE, null)
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun setState(newState: KidzoState, payload: Any?) {
        mainThread {
            _state.value = newState
            when (newState) {
                KidzoState.RECOMMENDATIONS -> {
                    if (payload is List<*>) {
                        _cards.value = payload as List<ContentCard>
                    }
                }
                KidzoState.CHATTING -> {
                    if (payload is String) {
                        _chatMessages.value = _chatMessages.value + payload
                    }
                }
                KidzoState.IDLE -> {
                    _cards.value = emptyList()
                    _chatMessages.value = emptyList()
                }
                else -> { /* THINKING / ERROR — no extra state */ }
            }
        }
    }

    private fun enrich(parsed: List<ContentCard>, itemMap: Map<String, ContentItem>): List<ContentCard> {
        return parsed.map { card ->
            val item = itemMap[card.contentId]
            val emoji = if (item != null && item.emoji.isNotEmpty()) item.emoji else "🐥"
            ContentCard(card.contentId, card.displayText, emoji, typeOf(card.contentId))
        }
    }

    private fun typeOf(contentId: String): String =
        if (contentId.startsWith("song-")) "Qo’shiq" else "Ertak"

    private fun buildOfflineResponse(lower: String): String {
        if (lower.matches(Regex(".*(salom|assalom|hello|hi|привет).*")))
            return "Salom! 🐥 Men Kidzo — sening bilim do’sting! Ertak eshitmoqchimisan?"

        if (lower.matches(Regex(".*(ertak|hikoya|ayt|eshit|story|сказку|расскажи).*"))) {
            var results = contentFilter.getFiltered(lower)
            if (results.isEmpty()) results = contentFilter.getTop5()
            if (results.isNotEmpty()) {
                val item = results[(Math.random() * results.size).toInt()]
                return "📚 ${item.emoji} \"${item.titleUz}\" ertagi! Ochish uchun kartochkaga bos 👆"
            }
        }

        if (lower.matches(Regex(".*(hayvon|sher|fil|mushuk|qush|animal|lion|жив).*"))) {
            val facts = arrayOf(
                "🦁 Sher — o’rmonning qiroli! U juda kuchli.",
                "🐘 Fil — quruqlikdagi eng katta hayvon!",
                "🦒 Jirafa — eng bo’yi baland hayvon. Bo’yni 2 metrga yetadi!",
                "🐬 Delfinlar — dengizning eng aqlli jonzotlari."
            )
            return facts[(Math.random() * facts.size).toInt()]
        }

        if (lower.matches(Regex(".*(rang|color|цвет|kamalak|qizil|yashil|ko’k).*")))
            return "🌈 Kamalakda 7 ta rang: qizil, to’q sariq, sariq, yashil, ko’k, moviy, binafsha!"

        if (lower.matches(Regex(".*(son|raqam|number|число|hisob|matematik|qo’sh).*")))
            return "🔢 1 dan 10 gacha: bir, ikki, uch, to’rt, besh, olti, yetti, sakkiz, to’qqiz, o’n!"

        if (lower.matches(Regex(".*(sayyora|planet|koinot|space|космос|oy|quyosh|moon).*")))
            return "🪐 Quyosh sistemasida 8 ta sayyora bor. Yer — uchinchisi!"

        if (lower.matches(Regex(".*(shakl|doira|kvadrat|shape|circle|square|фигур).*")))
            return "🔵 Doira, 🔶 uchburchak, 🟦 kvadrat — asosiy shakllar!"

        if (lower.matches(Regex(".*(meva|fruit|фрукт|olma|banan|apple|apelsin).*")))
            return "🍎 Olma, 🍌 banan, 🍊 apelsin — foydali mevalar!"

        return "🤔 Tushunmadim, lekin yordam bera olaman! \"ertak ayt\" de yoki hayvonlar, sayyoralar, ranglar haqida so’ra 📚"
    }

    private fun buildRecommendationPrompt(
        childName: String,
        lastContentId: String?,
        contentBlock: String
    ): String {
        return "Sen KidZone ilovasidagi \"Kidzo\" nomli mehribon qushchasan.\n" +
            "Faqat O’zbek tilida, qisqa va bolalarga mos tarzda gaplash.\n" +
            "Bolaning ismi: $childName.\n" +
            (if (lastContentId != null) "Oxirgi eshitgan kontenti: $lastContentId.\n" else "") +
            "\nQuyidagi kontentlardan $childName uchun 3 ta mos tavsiya tanlaydi:\n" +
            "$contentBlock\n" +
            "// Har qator formati: \"id|emoji|nomUz|kategoriya\"\n" +
            "\nHar bir tavsiyani quyidagi formatda yoz:\n" +
            "[OPEN:content-id] Kontent nomi — qisqa tavsif\n" +
            "\nBoshqa format ishlatma. Faqat ro’yxatdagi ID’larni ishlat."
    }
}
