package uz.kidzone.shared.kidzo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class KidzoAgent(
    private val gemini: GeminiCaller,
    private val contentFilter: ContentFilterProvider,
    private val scope: CoroutineScope,
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

    fun setOnContentOpen(cb: (String) -> Unit) {
        onContentOpen = cb
    }

    /** FAB pressed: IDLE → THINKING → RECOMMENDATIONS */
    fun requestRecommendations() {
        val gen = ++callGeneration
        setState(KidzoState.THINKING, null)

        val top5 = contentFilter.getTop5()
        val promptBlock = top5.joinToString("\n") { it.toPromptLine() }
        val prompt = buildRecommendationPrompt("Bolam", null, promptBlock)

        val itemMap = top5.associateBy { it.id }

        scope.launch {
            try {
                val text = gemini.call(prompt)
                if (gen != callGeneration) return@launch
                val parsed = ActionParser.parseRecommendations(text)
                var cardList = enrich(parsed, itemMap)
                if (cardList.isEmpty()) {
                    cardList = top5.map { item ->
                        ContentCard(item.id, item.titleUz, item.emoji, typeOf(item.id))
                    }
                }
                setState(KidzoState.RECOMMENDATIONS, cardList)
            } catch (e: Exception) {
                if (gen != callGeneration) return@launch
                setState(KidzoState.ERROR, e.message ?: "Error")
            }
        }
    }

    fun openContent(contentId: String) {
        scope.launch(Dispatchers.Main) { onContentOpen?.invoke(contentId) }
    }

    fun startChat() {
        setState(KidzoState.CHATTING, "Salom! Men Kidzo. Nima haqida gaplashamiz? 🐥")
    }

    fun sendChatMessage(userMessage: String) {
        val gen = ++callGeneration
        setState(KidzoState.THINKING, null)
        scope.launch {
            delay(400)
            if (gen != callGeneration) return@launch
            setState(KidzoState.CHATTING, buildOfflineResponse(userMessage.lowercase().trim()))
        }
    }

    fun dismiss() {
        callGeneration++
        setState(KidzoState.IDLE, null)
    }

    private fun setState(newState: KidzoState, payload: Any?) {
        scope.launch(Dispatchers.Main) {
            _state.value = newState
            when (newState) {
                KidzoState.RECOMMENDATIONS -> if (payload is List<*>) _cards.value = payload as List<ContentCard>
                KidzoState.CHATTING -> if (payload is String) _chatMessages.value = _chatMessages.value + payload
                KidzoState.IDLE -> {
                    _cards.value = emptyList()
                    _chatMessages.value = emptyList()
                }
                else -> {}
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

    private fun typeOf(contentId: String): String = if (contentId.startsWith("song-")) "Qo'shiq" else "Ertak"

    private fun buildOfflineResponse(lower: String): String {
        if (lower.contains("salom") || lower.contains("hi") || lower.contains("hello"))
            return "Salom! 🐥 Men Kidzo — sening bilim do'sting! Ertak eshitmoqchimisan?"
        
        val results = contentFilter.getFiltered(lower).ifEmpty { contentFilter.getTop5() }
        if (results.isNotEmpty()) {
            val item = results.random()
            return "📚 ${item.emoji} \"${item.titleUz}\" ertagi! Ochish uchun kartochkaga bos 👆"
        }
        return "🤔 Tushunmadim, lekin yordam bera olaman! \"ertak ayt\" de yoki hayvonlar haqida so'ra 📚"
    }

    private fun buildRecommendationPrompt(childName: String, lastContentId: String?, contentBlock: String): String {
        return "Sen KidZone ilovasidagi \"Kidzo\" nomli mehribon qushchasan.\n" +
            "Faqat O'zbek tilida, qisqa va bolalarga mos tarzda gaplash.\n" +
            "Bolaning ismi: $childName.\n" +
            (if (lastContentId != null) "Oxirgi eshitgan kontenti: $lastContentId.\n" else "") +
            "\nQuyidagi kontentlardan $childName uchun 3 ta mos tavsiya tanlaydi:\n" +
            "$contentBlock\n" +
            "// Har qator formati: \"id|emoji|nomUz|kategoriya\"\n" +
            "\nHar bir tavsiyani quyidagi formatda yoz:\n" +
            "[OPEN:content-id] Kontent nomi — qisqa tavsif\n" +
            "\nBoshqa format ishlatma. Faqat ro'yxatdagi ID'larni ishlat."
    }
}

interface ContentFilterProvider {
    fun getTop5(): List<ContentItem>
    fun getFiltered(query: String): List<ContentItem>
}
