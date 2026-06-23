package uz.kidzone.shared.ui.viewmodel

import kotlinx.coroutines.flow.StateFlow
import uz.kidzone.shared.kidzo.ContentCard
import uz.kidzone.shared.kidzo.KidzoAgent
import uz.kidzone.shared.kidzo.KidzoState

class KidzoViewModel(val agent: KidzoAgent) {
    val state: StateFlow<KidzoState> = agent.state
    val cards: StateFlow<List<ContentCard>> = agent.cards
    val chatMessages: StateFlow<List<String>> = agent.chatMessages

    fun requestRecommendations() { agent.requestRecommendations() }
    fun sendMessage(text: String) { agent.sendChatMessage(text) }
}
