package uz.kidzone.app.arch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

actual abstract class ViewModel actual constructor() {
    actual val viewModelScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    actual open fun onCleared() {
        viewModelScope.cancel()
    }
    
    // iOS (Swift) tomondan ViewModel ni tozalash uchun chaqiriladigan funksiya
    fun clear() {
        onCleared()
    }
}
