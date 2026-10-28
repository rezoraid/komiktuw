package com.komikta.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.core.network.ApiConfig
import com.komikta.domain.model.Comic
import com.komikta.domain.usecase.GetLatestComicsUseCase
import com.komikta.domain.usecase.GetPopularComicsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val latestComics: List<Comic> = emptyList(),
    val popularComics: List<Comic> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getLatestComics: GetLatestComicsUseCase,
    private val getPopularComics: GetPopularComicsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val source = ApiConfig.DEFAULT_SOURCE

            val latestResult = getLatestComics(source, page = 1)
            val popularResult = getPopularComics(source)

            val latest = (latestResult as? Result.Success)?.data.orEmpty()
            val popular = (popularResult as? Result.Success)?.data.orEmpty()

            val error = when {
                latestResult is Result.Error -> latestResult.message
                popularResult is Result.Error -> popularResult.message
                else -> null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    latestComics = latest,
                    popularComics = popular,
                    errorMessage = error
                )
            }
        }
    }
}
