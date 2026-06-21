package uz.kidzone.app.kidzo

interface KidzoStateListener {
    fun onStateChanged(newState: KidzoState, payload: Any?)
    fun onActionRequested(contentId: String)
}
