package com.komikta.feature.admin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.data.local.SourcePrefs
import com.komikta.data.remote.dto.AdminComicDto
import com.komikta.data.repository.CommunityRepository
import com.komikta.domain.model.Comic
import com.komikta.domain.model.SearchParams
import com.komikta.domain.model.canonicalGenres
import com.komikta.domain.repository.ComicRepository
import com.komikta.domain.usecase.GetLatestComicsUseCase
import com.komikta.domain.usecase.SearchComicsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------------------------------------------------------------------------------------
// Tab "Komik": cari komik (Manta, Shinigami, Lumos) lalu buka editornya.
// ---------------------------------------------------------------------------------------------

data class AdminComicsUi(
    val query: String = "",
    val items: List<Comic> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AdminComicsViewModel @Inject constructor(
    private val latest: GetLatestComicsUseCase,
    private val search: SearchComicsUseCase
) : ViewModel() {
    private val _ui = MutableStateFlow(AdminComicsUi())
    val ui: StateFlow<AdminComicsUi> = _ui.asStateFlow()
    private var job: Job? = null

    init { load("") }

    fun setQuery(q: String) {
        _ui.update { it.copy(query = q) }
        load(q.trim(), 350)
    }

    fun reload() = load(_ui.value.query.trim())

    private fun load(q: String, wait: Long = 0) {
        job?.cancel()
        job = viewModelScope.launch {
            if (wait > 0) delay(wait)
            _ui.update { it.copy(loading = it.items.isEmpty(), error = null) }
            val flow = if (q.isBlank()) latest.observe(SourcePrefs.ALL, 1) else search.observe(SearchParams(SourcePrefs.ALL, q, 1))
            flow.collect { r ->
                when (r) {
                    is Result.Success -> _ui.update { it.copy(loading = false, items = r.data, error = null) }
                    is Result.Empty -> _ui.update { it.copy(loading = false, items = emptyList(), error = null) }
                    is Result.Error -> _ui.update { if (it.items.isEmpty()) it.copy(loading = false, error = r.message) else it.copy(loading = false) }
                    else -> Unit
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Editor komik: judul, cover, deskripsi, genre, tipe, status, penulis.
// ---------------------------------------------------------------------------------------------

val EditableTypes = listOf("manhwa" to "Manhwa", "manhua" to "Manhua", "manga" to "Manga")
val EditableStatuses = listOf("ongoing" to "Ongoing", "completed" to "Completed", "hiatus" to "Hiatus", "cancelled" to "Cancelled")

data class ComicEditorUi(
    val loading: Boolean = true,
    val error: String? = null,
    val comic: AdminComicDto? = null,
    val title: String = "",
    val cover: String = "",
    val synopsis: String = "",
    val genres: String = "",
    val type: String = "",
    val status: String = "",
    val author: String = "",
    val artist: String = "",
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val uploadingCover: Boolean = false,
    val notice: String? = null
) {
    val canSave: Boolean get() = dirty && !saving && !uploadingCover && title.isNotBlank()
}

@HiltViewModel
class ComicEditorViewModel @Inject constructor(
    private val repo: CommunityRepository,
    private val comics: ComicRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val source: String = savedStateHandle["source"] ?: ""
    val slug: String = savedStateHandle["slug"] ?: ""

    private val _ui = MutableStateFlow(ComicEditorUi())
    val ui: StateFlow<ComicEditorUi> = _ui.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = it.comic == null, error = null) }
            when (val r = repo.adminComic(source, slug)) {
                is Result.Success -> _ui.update { fill(it, r.data).copy(loading = false) }
                is Result.Error -> _ui.update { it.copy(loading = false, error = r.message) }
                else -> _ui.update { it.copy(loading = false) }
            }
        }
    }

    private fun fill(st: ComicEditorUi, c: AdminComicDto) = st.copy(
        comic = c,
        title = c.title,
        cover = c.cover.orEmpty(),
        synopsis = c.synopsis.orEmpty(),
        genres = c.genres.joinToString(", "),
        type = c.type.orEmpty().lowercase(),
        status = c.status.orEmpty().lowercase(),
        author = c.author.orEmpty(),
        artist = c.artist.orEmpty(),
        dirty = false
    )

    private inline fun edit(block: (ComicEditorUi) -> ComicEditorUi) = _ui.update { block(it).copy(dirty = true, notice = null) }

    fun setTitle(v: String) = edit { it.copy(title = v) }
    fun setCover(v: String) = edit { it.copy(cover = v.trim()) }
    fun setSynopsis(v: String) = edit { it.copy(synopsis = v) }
    fun setGenres(v: String) = edit { it.copy(genres = v) }
    fun setType(v: String) = edit { it.copy(type = v) }
    fun setStatus(v: String) = edit { it.copy(status = v) }
    fun setAuthor(v: String) = edit { it.copy(author = v) }
    fun setArtist(v: String) = edit { it.copy(artist = v) }
    fun clearNotice() = _ui.update { it.copy(notice = null) }

    fun pickCover(uri: Uri) {
        viewModelScope.launch {
            _ui.update { it.copy(uploadingCover = true, notice = null) }
            val prepared = ImagePrep.read(context, uri)
            if (prepared == null) {
                _ui.update { it.copy(uploadingCover = false, notice = "Gambar tidak bisa dibaca") }
                return@launch
            }
            when (val r = repo.adminUpload(prepared.bytes, prepared.mime, prepared.name)) {
                is Result.Success -> _ui.update { it.copy(uploadingCover = false, cover = r.data, dirty = true) }
                is Result.Error -> _ui.update { it.copy(uploadingCover = false, notice = r.message) }
                else -> _ui.update { it.copy(uploadingCover = false) }
            }
        }
    }

    fun save() {
        val s = _ui.value
        if (!s.canSave) return
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, notice = null) }
            val genreList = canonicalGenres(s.genres.split(',', ';', '\n'))
            when (val r = repo.adminSaveComic(source, slug, s.title.trim(), s.cover.trim(), s.synopsis.trim(), genreList, s.type, s.status, s.author.trim(), s.artist.trim())) {
                is Result.Success -> {
                    comics.invalidate(source, slug) // cache dibuang + Home/Explore/Detail dimuat ulang
                    _ui.update { fill(it, r.data).copy(saving = false, notice = "Tersimpan. Aplikasi sudah diperbarui.") }
                }
                is Result.Error -> _ui.update { it.copy(saving = false, notice = r.message) }
                else -> _ui.update { it.copy(saving = false) }
            }
        }
    }

    fun resetToSource() {
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, notice = null) }
            when (val r = repo.adminResetComic(source, slug)) {
                is Result.Success -> {
                    comics.invalidate(source, slug)
                    _ui.update { fill(it, r.data).copy(saving = false, notice = "Dikembalikan ke data asli") }
                }
                is Result.Error -> _ui.update { it.copy(saving = false, notice = r.message) }
                else -> _ui.update { it.copy(saving = false) }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Editor gambar chapter: ganti, hapus, tambah, atur urutan.
// ---------------------------------------------------------------------------------------------

/** [url] kosong selagi gambar masih diunggah. [id] hanya untuk key daftar, tidak dikirim ke server. */
data class PageItem(val id: Long, val url: String, val busy: Boolean = false)

data class PagesEditorUi(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<PageItem> = emptyList(),
    val edited: Boolean = false,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val notice: String? = null
) {
    val busy: Boolean get() = items.any { it.busy }
    val canSave: Boolean get() = dirty && !saving && !busy && items.isNotEmpty()
}

@HiltViewModel
class PagesEditorViewModel @Inject constructor(
    private val repo: CommunityRepository,
    private val comics: ComicRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val source: String = savedStateHandle["source"] ?: ""
    val slug: String = savedStateHandle["slug"] ?: ""
    val chapter: String = savedStateHandle["chapter"] ?: ""

    private val _ui = MutableStateFlow(PagesEditorUi())
    val ui: StateFlow<PagesEditorUi> = _ui.asStateFlow()
    private var nextId = 1L

    init { load() }

    private fun items(urls: List<String>) = urls.map { PageItem(nextId++, it) }

    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = it.items.isEmpty(), error = null) }
            when (val r = repo.adminPages(source, slug, chapter)) {
                is Result.Success -> _ui.update { it.copy(loading = false, items = items(r.data.pages), edited = r.data.edited, dirty = false) }
                is Result.Error -> _ui.update { it.copy(loading = false, error = r.message) }
                else -> _ui.update { it.copy(loading = false) }
            }
        }
    }

    fun clearNotice() = _ui.update { it.copy(notice = null) }

    private inline fun change(block: (List<PageItem>) -> List<PageItem>) =
        _ui.update { it.copy(items = block(it.items), dirty = true, notice = null) }

    /** Geser satu posisi ke atas (-1) atau ke bawah (+1). */
    fun move(index: Int, delta: Int) = change { list ->
        val to = index + delta
        if (index !in list.indices || to !in list.indices) list
        else list.toMutableList().apply { add(to, removeAt(index)) }
    }

    fun delete(index: Int) = change { list -> if (index in list.indices) list.toMutableList().apply { removeAt(index) } else list }

    fun replace(index: Int, uri: Uri) {
        val id = _ui.value.items.getOrNull(index)?.id ?: return
        viewModelScope.launch {
            setBusy(id, true)
            val url = upload(uri)
            if (url != null) {
                _ui.update { st -> st.copy(items = st.items.map { if (it.id == id) it.copy(url = url, busy = false) else it }, dirty = true) }
            } else setBusy(id, false)
        }
    }

    /** Tambah gambar di akhir (boleh banyak sekaligus); diunggah berurutan agar ringan. */
    fun add(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val placeholders = uris.map { PageItem(nextId++, "", busy = true) }
        _ui.update { it.copy(items = it.items + placeholders, dirty = true, notice = null) }
        viewModelScope.launch {
            placeholders.zip(uris).forEach { (p, uri) ->
                val url = upload(uri)
                _ui.update { st ->
                    st.copy(items = if (url != null) st.items.map { if (it.id == p.id) it.copy(url = url, busy = false) else it } else st.items.filter { it.id != p.id })
                }
            }
        }
    }

    private fun setBusy(id: Long, busy: Boolean) =
        _ui.update { st -> st.copy(items = st.items.map { if (it.id == id) it.copy(busy = busy) else it }) }

    private suspend fun upload(uri: Uri): String? {
        val prepared = ImagePrep.read(context, uri)
        if (prepared == null) {
            _ui.update { it.copy(notice = "Gambar tidak bisa dibaca") }
            return null
        }
        return when (val r = repo.adminUpload(prepared.bytes, prepared.mime, prepared.name)) {
            is Result.Success -> r.data.takeIf { it.isNotBlank() }
            is Result.Error -> { _ui.update { it.copy(notice = r.message) }; null }
            else -> null
        }
    }

    fun save() {
        val s = _ui.value
        if (!s.canSave) return
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, notice = null) }
            when (val r = repo.adminSavePages(source, slug, chapter, s.items.map { it.url })) {
                is Result.Success -> {
                    comics.invalidate(source, slug, chapter)
                    _ui.update { it.copy(saving = false, items = items(r.data.pages), edited = r.data.edited, dirty = false, notice = "Tersimpan. Reader langsung memakai urutan baru.") }
                }
                is Result.Error -> _ui.update { it.copy(saving = false, notice = r.message) }
                else -> _ui.update { it.copy(saving = false) }
            }
        }
    }

    fun resetToSource() {
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, notice = null) }
            when (val r = repo.adminResetPages(source, slug, chapter)) {
                is Result.Success -> {
                    comics.invalidate(source, slug, chapter)
                    _ui.update { it.copy(saving = false, items = items(r.data.pages), edited = false, dirty = false, notice = "Dikembalikan ke gambar asli") }
                }
                is Result.Error -> _ui.update { it.copy(saving = false, notice = r.message) }
                else -> _ui.update { it.copy(saving = false) }
            }
        }
    }
}
