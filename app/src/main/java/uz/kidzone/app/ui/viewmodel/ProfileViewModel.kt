package uz.kidzone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

    init {
        viewModelScope.launch {
            _activeProfile.value = repository.getActiveProfile()
        }
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
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ProfileViewModel(repository) as T
}
