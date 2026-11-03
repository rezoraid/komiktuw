package com.komikta.domain.usecase

import com.komikta.core.common.Result
import com.komikta.domain.model.BrowseParams
import com.komikta.domain.model.ChapterContent
import com.komikta.domain.model.Comic
import com.komikta.domain.model.ComicDetail
import com.komikta.domain.model.SearchParams
import com.komikta.domain.model.Source
import com.komikta.domain.repository.ComicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSourcesUseCase @Inject constructor(private val repository: ComicRepository) {
    suspend operator fun invoke(): Result<List<Source>> = repository.getSources()
}

class GetLatestComicsUseCase @Inject constructor(private val repository: ComicRepository) {
    suspend operator fun invoke(source: String, page: Int = 1): Result<List<Comic>> = repository.getLatest(source, page)
    fun observe(source: String, page: Int = 1, refresh: Boolean = false): Flow<Result<List<Comic>>> =
        repository.observeLatest(source, page, refresh)
}

class GetPopularComicsUseCase @Inject constructor(private val repository: ComicRepository) {
    suspend operator fun invoke(source: String): Result<List<Comic>> = repository.getPopular(source)
    fun observe(source: String, refresh: Boolean = false): Flow<Result<List<Comic>>> =
        repository.observePopular(source, refresh)
}

class BrowseComicsUseCase @Inject constructor(private val repository: ComicRepository) {
    suspend operator fun invoke(params: BrowseParams): Result<List<Comic>> = repository.browse(params)
    fun observe(params: BrowseParams, refresh: Boolean = false): Flow<Result<List<Comic>>> =
        repository.observeBrowse(params, refresh)
}

class SearchComicsUseCase @Inject constructor(private val repository: ComicRepository) {
    suspend operator fun invoke(params: SearchParams): Result<List<Comic>> = repository.search(params)
    fun observe(params: SearchParams): Flow<Result<List<Comic>>> = repository.observeSearch(params)
}

class GetGenresUseCase @Inject constructor(private val repository: ComicRepository) {
    suspend operator fun invoke(source: String): Result<List<String>> = repository.getGenres(source)
    fun observe(source: String): Flow<Result<List<String>>> = repository.observeGenres(source)
}

class GetComicDetailUseCase @Inject constructor(private val repository: ComicRepository) {
    suspend operator fun invoke(source: String, slug: String): Result<ComicDetail> = repository.getComicDetail(source, slug)
    fun observe(source: String, slug: String, refresh: Boolean = false): Flow<Result<ComicDetail>> =
        repository.observeDetail(source, slug, refresh)
}

class GetChapterContentUseCase @Inject constructor(private val repository: ComicRepository) {
    suspend operator fun invoke(source: String, comicSlug: String, chapterSlug: String): Result<ChapterContent> =
        repository.getChapterContent(source, comicSlug, chapterSlug)

    fun observe(source: String, comicSlug: String, chapterSlug: String, refresh: Boolean = false): Flow<Result<ChapterContent>> =
        repository.observeChapter(source, comicSlug, chapterSlug, refresh)
}
