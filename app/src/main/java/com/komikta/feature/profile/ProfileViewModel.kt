package com.komikta.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.data.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(val busy: Boolean = false, val message: String? = null)

@HiltViewModel
class ProfileViewModel @Inject constructor(private val repo: CommunityRepository) : ViewModel() {
    val user = repo.user
    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui.asStateFlow()

    init { if (repo.user.value != null) viewModelScope.launch { repo.refreshMe() } }

    fun message(msg: String?) { _ui.value = _ui.value.copy(message = msg) }

    fun signIn(idToken: String) {
        viewModelScope.launch {
            _ui.value = ProfileUiState(busy = true)
            when (val r = repo.signInWithGoogle(idToken)) {
                is Result.Success -> _ui.value = ProfileUiState()
                is Result.Error -> _ui.value = ProfileUiState(message = r.message)
                else -> _ui.value = ProfileUiState()
            }
        }
    }

    fun signOut() {
        repo.signOut()
        _ui.value = ProfileUiState()
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _ui.value = ProfileUiState(busy = true)
            when (val r = repo.deleteAccount()) {
                is Result.Success -> _ui.value = ProfileUiState(message = "Akun dihapus")
                is Result.Error -> _ui.value = ProfileUiState(message = r.message)
                else -> _ui.value = ProfileUiState()
            }
        }
    }

    fun changeUsername(username: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _ui.value = ProfileUiState(busy = true)
            when (val r = repo.updateUsername(username)) {
                is Result.Success -> { _ui.value = ProfileUiState(message = "Username diperbarui"); onDone(true) }
                is Result.Error -> { _ui.value = ProfileUiState(message = r.message); onDone(false) }
                else -> { _ui.value = ProfileUiState(); onDone(false) }
            }
        }
    }
}
