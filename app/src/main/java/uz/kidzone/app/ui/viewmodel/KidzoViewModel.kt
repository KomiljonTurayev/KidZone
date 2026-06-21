package uz.kidzone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import uz.kidzone.app.kidzo.ContentCard
import uz.kidzone.app.kidzo.KidzoAgent
import uz.kidzone.app.kidzo.KidzoState

class KidzoViewModel(val agent: KidzoAgent) : ViewModel() {
    val state: StateFlow<KidzoState> = agent.state
    val cards: StateFlow<List<ContentCard>> = agent.cards
    val chatMessages: StateFlow<List<String>> = agent.chatMessages

    fun requestRecommendations() { agent.requestRecommendations() }
    fun sendMessage(text: String) { agent.sendChatMessage(text) }
}
