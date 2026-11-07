package com.komikta.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.data.remote.dto.CatalogStatusDto
import com.komikta.data.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminCatalogState(
    val loading: Boolean = true,
    val status: CatalogStatusDto? = null,
    val error: String? = null,
    val busy: Boolean = false,
    val notice: String? = null,
    val source: String = "manta",
    val slug: String = ""
)

/** Panel Katalog: melihat kelengkapan data komik dan memicu sinkronisasi dari app. */
@HiltViewModel
class AdminCatalogViewModel @Inject constructor(private val repo: CommunityRepository) : ViewModel() {
    private val _ui = MutableStateFlow(AdminCatalogState())
    val ui: StateFlow<AdminCatalogState> = _ui.asStateFlow()

    init { load() }

    fun setSource(v: String) = _ui.update { it.copy(source = v) }
    fun setSlug(v: String) = _ui.update { it.copy(slug = v.trim().take(160)) }
    fun clearNotice() = _ui.update { it.copy(notice = null) }

    fun load(silent: Boolean = false) = viewModelScope.launch {
        if (!silent) _ui.update { it.copy(loading = true, error = null) }
        when (val r = repo.adminCatalogStatus()) {
            is Result.Success -> _ui.update { it.copy(loading = false, status = r.data, error = null) }
            is Result.Error -> _ui.update { it.copy(loading = false, error = if (silent && it.status != null) null else r.message) }
            else -> _ui.update { it.copy(loading = false) }
        }
    }

    /** Sinkron massal di server (crawl). [source] kosong = semua sumber. */
    fun syncSource(source: String) = viewModelScope.launch {
        _ui.update { it.copy(busy = true, notice = null) }
        when (val r = repo.adminCatalogSync(source, "", 8)) {
            is Result.Success -> {
                _ui.update { it.copy(busy = false, notice = "Sinkronisasi ${if (source.isEmpty()) "semua sumber" else sourceLabel(source)} dimulai di server") }
                delay(3000); load(silent = true)
            }
            is Result.Error -> _ui.update { it.copy(busy = false, notice = r.message) }
            else -> _ui.update { it.copy(busy = false) }
        }
    }

    /** Ambil ulang satu komik dari sumbernya sekarang juga. */
    fun syncOne() = viewModelScope.launch {
        val st = _ui.value
        if (st.slug.isEmpty()) return@launch
        _ui.update { it.copy(busy = true, notice = null) }
        when (val r = repo.adminCatalogSync(st.source, st.slug, 0)) {
            is Result.Success -> {
                _ui.update { it.copy(busy = false, notice = "Komik berhasil disinkronkan", slug = "") }
                load(silent = true)
            }
            is Result.Error -> _ui.update { it.copy(busy = false, notice = r.message) }
            else -> _ui.update { it.copy(busy = false) }
        }
    }
}

internal fun sourceLabel(id: String): String = when (id) {
    "manta" -> "Manta"
    "shinigami" -> "Shinigami"
    "lumos" -> "Lumos"
    else -> id.replaceFirstChar { it.uppercase() }
}
