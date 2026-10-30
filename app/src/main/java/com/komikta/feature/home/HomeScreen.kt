package com.komikta.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komikta.R
import com.komikta.domain.model.Comic
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.ComicCard
import com.komikta.ui.components.PosterCard
import com.komikta.ui.components.PosterRowSkeleton
import com.komikta.ui.components.historyChapterLabel
import com.komikta.ui.components.relativeTime
import com.komikta.ui.theme.Ui
import androidx.compose.runtime.getValue

private val Pad = 14.dp
private val Gap = 10.dp

@Composable
fun HomeScreen(
    onComicClick: (source: String, slug: String) -> Unit,
    onReadClick: (source: String, comicSlug: String, chapterSlug: String) -> Unit,
    onSearchClick: () -> Unit,
    onContinueViewAll: () -> Unit,
    onUpdateViewAll: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.continueReading.collectAsStateWithLifecycle()

    when {
        uiState.isLoading && uiState.latestComics.isEmpty() -> {
            Column(Modifier.fillMaxSize().background(Ui.Bg)) {
                HomeHeader(onSearchClick)
                PosterRowSkeleton(rows = 3)
            }
        }
        uiState.errorMessage != null && uiState.latestComics.isEmpty() -> {
            Box(Modifier.fillMaxSize().background(Ui.Bg), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.errorMessage ?: "Error", color = Ui.Text2)
                    TextButton(onClick = viewModel::loadHome) { Text("Retry", color = Color.White) }
                }
            }
        }
        else -> {
            val latest = uiState.latestComics
            val popular = remember(uiState.popularComics) { uiState.popularComics.take(10) }
            val updateRows = remember(latest) { latest.take(12).chunked(3) }
            val forYou = remember(latest) { latest.drop(12).sortedByDescending { it.rating ?: 0.0 }.take(10) }
            val now = remember { System.currentTimeMillis() }
            val cardWidth = (LocalConfiguration.current.screenWidthDp.dp - Pad * 2 - Gap * 2) / 3

            LazyColumn(
                Modifier.fillMaxSize().background(Ui.Bg),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item(key = "header", contentType = "header") { HomeHeader(onSearchClick) }

                if (history.isNotEmpty()) {
                    item(key = "continue-title", contentType = "title") { SectionHeader("Lanjut baca", onContinueViewAll) }
                    item(key = "continue-row", contentType = "row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Pad),
                            horizontalArrangement = Arrangement.spacedBy(Gap)
                        ) {
                            items(history, key = { it.id }, contentType = { "card" }) { h ->
                                PosterCard(
                                    title = h.comicTitle,
                                    coverUrl = h.coverUrl,
                                    badge = historyChapterLabel(h.lastChapterNumber),
                                    meta = relativeTime(h.lastReadAt, now),
                                    modifier = Modifier.width(cardWidth),
                                    onClick = {
                                        val chapter = h.lastChapterSlug
                                        if (chapter.isNullOrBlank()) onComicClick(h.source, h.comicSlug)
                                        else onReadClick(h.source, h.comicSlug, chapter)
                                    }
                                )
                            }
                        }
                    }
                }

                if (popular.isNotEmpty()) {
                    item(key = "popular-title", contentType = "title") { SectionHeader("Populer", null) }
                    item(key = "popular-row", contentType = "row") { PosterRow(popular, cardWidth, onComicClick) }
                }

                if (updateRows.isNotEmpty()) {
                    item(key = "update-title", contentType = "title") { SectionHeader("Update terbaru", onUpdateViewAll) }
                    items(
                        count = updateRows.size,
                        key = { "update:${updateRows[it].first().source}:${updateRows[it].first().slug}" },
                        contentType = { "grid-row" }
                    ) { i ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = Pad).padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(Gap),
                            verticalAlignment = Alignment.Top
                        ) {
                            updateRows[i].forEach { comic ->
                                ComicCard(comic, { onComicClick(comic.source, comic.slug) }, Modifier.width(cardWidth))
                            }
                        }
                    }
                }

                if (forYou.isNotEmpty()) {
                    item(key = "foryou-title", contentType = "title") { SectionHeader("Untukmu", null) }
                    item(key = "foryou-row", contentType = "row") { PosterRow(forYou, cardWidth, onComicClick) }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onSearch: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = Pad, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = "Komikta",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = 96.dp, height = 32.dp)
        )
        Box(Modifier.weight(1f))
        IconButton(onClick = onSearch) {
            Icon(AppIcons.search, "Cari", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, onAction: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(start = Pad, end = Pad - 6.dp, top = 18.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        if (onAction != null) {
            Text(
                "Lihat semua",
                fontSize = 13.sp,
                color = Ui.Text2,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PosterRow(
    comics: List<Comic>,
    cardWidth: Dp,
    onComicClick: (String, String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Pad),
        horizontalArrangement = Arrangement.spacedBy(Gap),
        verticalAlignment = Alignment.Top
    ) {
        items(comics, key = { "${it.source}:${it.slug}" }, contentType = { "card" }) { comic ->
            ComicCard(comic, { onComicClick(comic.source, comic.slug) }, Modifier.width(cardWidth))
        }
    }
}
