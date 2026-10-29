package uz.kidzone.app.ui.viewmodel

import uz.kidzone.app.arch.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.data.ProfileRepository

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    val profiles: StateFlow<List<ProfileEntity>> = repository.profiles.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _activeProfile = MutableStateFlow<ProfileEntity?>(null)
    val activeProfile: StateFlow<ProfileEntity?> = _activeProfile.asStateFlow()

    // Follows Room, so changes from another device or the family owner show up here. If the
    // active profile is gone (another family's profiles replaced it), the default one takes over.
    init {
        viewModelScope.launch {
            repository.profiles.collect { profiles -> _activeProfile.value = activeIn(profiles) }
        }
    }

    private fun activeIn(profiles: List<ProfileEntity>): ProfileEntity? {
        val id = repository.getActiveProfileId() ?: return null
        val active = profiles.firstOrNull { it.id == id }
            ?: profiles.firstOrNull { it.isDefault }
            ?: profiles.firstOrNull()
        if (active != null && active.id != id) repository.setActiveProfileId(active.id)
        return active
    }

    fun setActiveProfile(profile: ProfileEntity) {
        repository.setActiveProfileId(profile.id)
        _activeProfile.value = profile
    }

    fun insertProfile(profile: ProfileEntity) {
        viewModelScope.launch { repository.insert(profile) }
    }

    fun updateProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            repository.update(profile)
            if (_activeProfile.value?.id == profile.id) {
                _activeProfile.value = profile
            }
        }
    }

    /**
     * Sets the parent PIN. There is one parent per family, so every profile gets the same PIN:
     * whichever child is active, the same PIN opens the parent area.
     */
    fun setParentPin(pinHash: String) {
        viewModelScope.launch {
            repository.profiles.first().forEach { repository.update(it.copy(pinHash = pinHash)) }
            _activeProfile.value = _activeProfile.value?.copy(pinHash = pinHash)
        }
    }

    fun deleteProfile(profile: ProfileEntity, onSwitched: (ProfileEntity?) -> Unit) {
        viewModelScope.launch {
            repository.delete(profile)
            if (_activeProfile.value?.id == profile.id) {
                val next = repository.profiles.first().firstOrNull { it.id != profile.id }
                if (next != null) setActiveProfile(next)
                onSwitched(next)
            } else {
                onSwitched(_activeProfile.value)
            }
        }
    }
}

class ProfileViewModelFactory(private val repository: ProfileRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        ProfileViewModel(repository) as T
}
