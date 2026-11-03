package com.komikta.feature.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import coil.ImageLoader
import coil.request.ImageRequest
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.data.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ProfileUiState(val busy: Boolean = false, val bannerBusy: Boolean = false, val message: String? = null)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: CommunityRepository,
    @ApplicationContext private val appContext: Context,
    private val imageLoader: ImageLoader
) : ViewModel() {
    val user = repo.user
    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (repo.user.value != null) viewModelScope.launch { repo.refreshMe() }
    }


    fun busy(b: Boolean) { _ui.value = _ui.value.copy(busy = b) }

    fun message(msg: String?) { _ui.value = _ui.value.copy(message = msg) }

    fun signIn(idToken: String) {
        viewModelScope.launch {
            _ui.value = ProfileUiState(busy = true)
            when (val r = repo.signInWithGoogle(idToken)) {
                is Result.Success -> { _ui.value = ProfileUiState(); repo.refreshMe() }
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

    fun setHistoryVisibility(isPublic: Boolean) {
        viewModelScope.launch {
            when (val r = repo.setReadingHistoryPublic(isPublic)) {
                is Result.Success -> _ui.value = ProfileUiState(message = if (isPublic) "Riwayat sekarang publik" else "Riwayat sekarang privat")
                is Result.Error -> _ui.value = ProfileUiState(message = r.message)
                else -> Unit
            }
        }
    }

    fun changeAvatar(uri: Uri) {
        viewModelScope.launch {
            val prepared = withContext(Dispatchers.IO) { runCatching { AvatarProcessor.prepare(appContext, uri) } }
            val avatar = prepared.getOrNull()
            if (avatar == null) {
                _ui.value = ProfileUiState(message = prepared.exceptionOrNull()?.message ?: "Gagal membaca foto")
                return@launch
            }
            when (val r = repo.updateAvatar(avatar.dataUri, avatar.previewUrl, ::preloadAvatar)) {
                is Result.Success -> _ui.value = ProfileUiState(message = "Foto profil diperbarui")
                is Result.Error -> _ui.value = ProfileUiState(message = r.message)
                else -> Unit
            }
        }
    }

    fun changeBanner(uri: Uri) {
        if (_ui.value.bannerBusy) return
        viewModelScope.launch {
            _ui.value = ProfileUiState(bannerBusy = true)
            val prepared = withContext(Dispatchers.IO) { runCatching { AvatarProcessor.prepareBanner(appContext, uri) } }
            val data = prepared.getOrNull()
            if (data == null) {
                _ui.value = ProfileUiState(message = prepared.exceptionOrNull()?.message ?: "Gagal membaca gambar")
                return@launch
            }
            _ui.value = when (val r = repo.updateBanner(data)) {
                is Result.Success -> ProfileUiState(message = "Banner diperbarui")
                is Result.Error -> ProfileUiState(message = r.message)
                else -> ProfileUiState()
            }
        }
    }

    fun removeBanner() {
        if (_ui.value.bannerBusy) return
        viewModelScope.launch {
            _ui.value = ProfileUiState(bannerBusy = true)
            _ui.value = when (val r = repo.removeBanner()) {
                is Result.Success -> ProfileUiState(message = "Banner dihapus")
                is Result.Error -> ProfileUiState(message = r.message)
                else -> ProfileUiState()
            }
        }
    }

    fun changeUsername(username: String, onDone: (Boolean) -> Unit = {}) {
        if (username == repo.user.value?.username) { onDone(true); return }
        viewModelScope.launch {
            when (val r = repo.updateUsername(username)) {
                is Result.Success -> { _ui.value = ProfileUiState(message = "Username diperbarui"); onDone(true) }
                is Result.Error -> { _ui.value = ProfileUiState(message = r.message); onDone(false) }
                else -> onDone(false)
            }
        }
    }

    private suspend fun preloadAvatar(url: String) {
        imageLoader.execute(ImageRequest.Builder(appContext).data(url).size(AVATAR_PX, AVATAR_PX).build())
    }

    private companion object {
        const val AVATAR_PX = 160
    }
}
