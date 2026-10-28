package com.komikta.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.domain.model.ChapterContent
import com.komikta.domain.usecase.GetChapterContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val isLoading: Boolean = true,
    val content: ChapterContent? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val getChapterContent: GetChapterContentUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val source: String = savedStateHandle["source"] ?: "lumos"
    private val comicSlug: String = savedStateHandle["comicSlug"] ?: ""
    private val chapterSlug: String = savedStateHandle["chapterSlug"] ?: ""

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        loadChapter()
    }

    fun loadChapter() {
        if (comicSlug.isBlank() || chapterSlug.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid chapter") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getChapterContent(source, comicSlug, chapterSlug)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, content = result.data)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
