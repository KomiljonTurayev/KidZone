package uz.kidzone.app.ui.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.kidzone.app.FamilyApiClient
import uz.kidzone.app.FamilyResult
import uz.kidzone.app.arch.ViewModel
import uz.kidzone.app.auth.AuthRepository

enum class DeleteAccountState { IDLE, DELETING, FAILED, DELETED }

/** Deletes the account (or the guest's session) on the server; the screen then wipes the device. */
class DeleteAccountViewModel(
    private val auth: AuthRepository,
    private val deleteOnServer: suspend () -> FamilyResult<Unit> = FamilyApiClient::deleteAccount,
) : ViewModel() {

    private val _state = MutableStateFlow(DeleteAccountState.IDLE)
    val state: StateFlow<DeleteAccountState> = _state.asStateFlow()

    fun delete() {
        if (_state.value == DeleteAccountState.DELETING) return
        _state.value = DeleteAccountState.DELETING
        viewModelScope.launch {
            // No session: nothing on the server. "Gone": it was deleted but the answer got lost.
            val done = auth.state.value.uid == null ||
                deleteOnServer() is FamilyResult.Ok ||
                auth.isAccountGone()
            _state.value = if (done) DeleteAccountState.DELETED else DeleteAccountState.FAILED
        }
    }
}
