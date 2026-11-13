package com.komikta.feature.home

import com.komikta.ui.components.ErrorState
import com.komikta.ui.components.EmptyState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komikta.R
import com.komikta.data.local.entity.ReadingHistoryEntity
import com.komikta.domain.model.Comic
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.Chip
import com.komikta.ui.components.ComicCard
import com.komikta.ui.components.CoverImage
import com.komikta.ui.components.NewBadge
import com.komikta.ui.components.PosterRowSkeleton
import com.komikta.ui.components.SegmentedTabs
import com.komikta.ui.components.TypeBadge
import com.komikta.ui.components.ComicStats
import com.komikta.ui.components.genreText
import com.komikta.ui.components.latestAgoText
import com.komikta.ui.components.latestChapterText
import com.komikta.ui.components.latestLine
import com.komikta.ui.components.historyChapterLabel
import com.komikta.ui.components.isFresh
import com.komikta.ui.components.relativeTime
import com.komikta.ui.components.displayType
import com.komikta.domain.model.originOf
import com.komikta.ui.theme.Ui

private val Pad = 14.dp
private val Gap = 10.dp
private val Origins = listOf("Manhwa", "Manhua", "Manga")

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

    LifecycleResumeEffect(Unit) {
        viewModel.revalidate()
        onPauseOrDispose {}
    }

    when {
        uiState.isLoading && uiState.latestComics.isEmpty() -> Column(Modifier.fillMaxSize().background(Ui.Bg)) {
            HomeHeader(onSearchClick)
            PosterRowSkeleton(rows = 3)
        }
        uiState.errorMessage != null && uiState.latestComics.isEmpty() -> Box(Modifier.fillMaxSize().background(Ui.Bg), contentAlignment = Alignment.Center) {
            ErrorState(uiState.errorMessage ?: "Terjadi kesalahan", { viewModel.loadHome(refresh = true) })
        }
        else -> {
            val latest = uiState.latestComics
            val popular = uiState.popularComics
            val all = remember(latest, popular) { (latest + popular).distinctBy { it.source to it.slug } }
            val hero = popular.firstOrNull() ?: latest.firstOrNull()
            // Rating tertinggi dulu; yang belum punya rating tetap ikut (urut views) supaya bagian ini tidak pernah kosong.
            val recommended = remember(all) { all.sortedWith(compareByDescending<Comic> { it.rating ?: 0.0 }.thenByDescending { it.viewCount ?: 0L }).take(8) }
            val updates = remember(latest) { latest.take(10) }
            // "Popular all time": views terbanyak lintas ketiga sumber; seri dengan views sama diurutkan menurut rating.
            val ranked = remember(all) { all.sortedWith(compareByDescending<Comic> { it.viewCount ?: 0L }.thenByDescending { it.rating ?: 0.0 }).take(9).chunked(3) }
            val genres = remember(all) { all.flatMap { it.genres }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(12).map { it.key }.sorted() }
            var genre by rememberSaveable { mutableStateOf<String?>(null) }
            var origin by rememberSaveable { mutableIntStateOf(0) }
            val activeGenre = genre ?: genres.firstOrNull()
            val byGenre = remember(all, activeGenre) { all.filter { activeGenre != null && activeGenre in it.genres }.take(9).chunked(3) }
            val byOrigin = remember(all, origin) { all.filter { originOf(it.type).equals(Origins[origin], true) }.take(12) }
            val now = remember { System.currentTimeMillis() }
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            val gridWidth = (screenWidth - Pad * 2 - Gap * 2) / 3
            val open = { c: Comic -> onComicClick(c.source, c.slug) }

            LazyColumn(Modifier.fillMaxSize().background(Ui.Bg), contentPadding = PaddingValues(bottom = 24.dp)) {
                item(key = "header") { HomeHeader(onSearchClick) }
                if (hero != null) item(key = "hero") { Hero(hero) { open(hero) } }
                if (history.isNotEmpty()) {
                    item(key = "continue-title") { SectionHeader("Continue reading", null, "History", onContinueViewAll) }
                    item(key = "continue-row") {
                        LazyRow(contentPadding = PaddingValues(horizontal = Pad), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                            items(history, key = { it.id }) { h ->
                                ContinueCard(h, now) {
                                    val chapter = h.lastChapterSlug
                                    if (chapter.isNullOrBlank()) onComicClick(h.source, h.comicSlug) else onReadClick(h.source, h.comicSlug, chapter)
                                }
                            }
                        }
                    }
                }
                if (recommended.isNotEmpty()) {
                    item(key = "rec-title") { SectionHeader("Recommended", "Picked by the Komikta team", "See all", onUpdateViewAll) }
                    item(key = "rec-row") {
                        LazyRow(contentPadding = PaddingValues(horizontal = Pad), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                            items(recommended, key = { "${it.source}:${it.slug}" }) { c -> WideCard(c, screenWidth * 0.62f) { open(c) } }
                        }
                    }
                }
                if (updates.isNotEmpty()) {
                    item(key = "update-title") { SectionHeader("Latest updates", null, "See all", onUpdateViewAll) }
                    items(updates, key = { "u:${it.source}:${it.slug}" }) { c -> UpdateRow(c) { open(c) } }
                }
                if (ranked.isNotEmpty()) {
                    item(key = "rank-title") { SectionHeader("Popular all time", "Most read on Komikta", "See all", onUpdateViewAll) }
                    item(key = "rank-row") {
                        LazyRow(contentPadding = PaddingValues(horizontal = Pad), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                            itemsIndexed(ranked, key = { page, _ -> "rank-page-$page" }) { page, column ->
                                Column(Modifier.width(screenWidth - 56.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    column.forEachIndexed { i, c -> RankRow(page * 3 + i + 1, c) { open(c) } }
                                }
                            }
                        }
                    }
                }
                if (genres.isNotEmpty()) {
                    item(key = "genre-title") { SectionHeader("Browse by genre", null, "See all", onUpdateViewAll) }
                    item(key = "genre-chips") {
                        LazyRow(contentPadding = PaddingValues(horizontal = Pad), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(genres, key = { it }) { g -> Chip(g, g == activeGenre, { genre = g }) }
                        }
                    }
                    item(key = "genre-gap") { Box(Modifier.height(16.dp)) }
                    items(byGenre.size, key = { "g:$activeGenre:$it" }) { i -> CardRow(byGenre[i], gridWidth, open) }
                }
                item(key = "origin-title") { SectionHeader("By origin", null, "See all", onUpdateViewAll) }
                item(key = "origin-tabs") { SegmentedTabs(Origins, origin, { origin = it }, Modifier.padding(horizontal = Pad), fill = false) }
                item(key = "origin-gap") { Box(Modifier.height(16.dp)) }
                if (byOrigin.isEmpty()) item(key = "origin-empty") {
                    EmptyState(AppIcons.search, "Belum ada ${Origins[origin]}", "Coba pilih jenis lain di atas.", compact = true)
                }
                item(key = "origin-row") {
                    LazyRow(contentPadding = PaddingValues(horizontal = Pad), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                        items(byOrigin, key = { "o:${it.source}:${it.slug}" }) { c -> ComicCard(c, { open(c) }, Modifier.width(92.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onSearch: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = Pad, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_logo), "Komikta", Modifier.size(width = 112.dp, height = 36.dp), contentScale = ContentScale.Fit, alignment = Alignment.CenterStart)
        Box(Modifier.weight(1f))
        IconButton(onClick = onSearch) { Icon(AppIcons.search, "Cari", tint = Color.White, modifier = Modifier.size(24.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String?, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = Pad, end = Pad, top = 22.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 4.dp, height = 18.dp).clip(CircleShape).background(Ui.Accent))
                Box(Modifier.width(10.dp))
                Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
            }
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = Ui.Text2, maxLines = 1, modifier = Modifier.padding(start = 14.dp, top = 2.dp))
        }
        Row(Modifier.clip(CircleShape).clickable(onClick = onAction).padding(start = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(action, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ui.Accent)
            Icon(AppIcons.chevronRight, null, tint = Ui.Accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Hero(comic: Comic, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = Pad).height(184.dp).clip(RoundedCornerShape(20.dp)).background(Ui.Card).clickable(onClick = onClick)) {
        CoverImage(comic.coverUrl, comic.title, Modifier.fillMaxSize(), 720, 420, Alignment.TopCenter)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color(0xFA181C21), 0.6f to Color(0xE6181C21), 1f to Color(0x4D181C21))))
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.68f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(displayType(comic).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Ui.Accent)
                ComicStats(comic, compact = true)
            }
            Text(comic.title, fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOf(genreText(comic, 2), latestChapterText(comic)).joinToString(" · "), fontSize = 13.sp, color = Ui.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Read now", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ui.OnAccent, modifier = Modifier.padding(top = 4.dp).clip(CircleShape).background(Ui.Accent).padding(horizontal = 22.dp, vertical = 9.dp))
        }
    }
}

@Composable
private fun ContinueCard(h: ReadingHistoryEntity, now: Long, onClick: () -> Unit) {
    Row(Modifier.width(265.dp).clip(RoundedCornerShape(16.dp)).background(Ui.Card).clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        CoverImage(h.coverUrl, h.comicTitle, Modifier.size(width = 50.dp, height = 74.dp).clip(RoundedCornerShape(8.dp)).background(Ui.Bg), 140, 210)
        Column(Modifier.weight(1f).height(74.dp).padding(start = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(h.comicTitle, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${historyChapterLabel(h.lastChapterNumber)} · ${relativeTime(h.lastReadAt, now)}", fontSize = 13.sp, color = Ui.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WideCard(c: Comic, width: Dp, onClick: () -> Unit) {
    Column(Modifier.width(width).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(width * 0.56f).clip(RoundedCornerShape(16.dp)).background(Ui.Card)) {
            CoverImage(c.coverUrl, c.title, Modifier.fillMaxSize(), 640, 360, Alignment.TopCenter)
            TypeBadge(displayType(c), Modifier.align(Alignment.TopStart).padding(10.dp))
            if (isFresh(c.chapters.firstOrNull()?.timeAgo)) NewBadge(Modifier.align(Alignment.TopEnd).padding(10.dp))
        }
        Text(c.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComicStats(c, compact = true)
            Text(genreText(c, 2), fontSize = 12.sp, color = Ui.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

@Composable
private fun UpdateRow(c: Comic, onClick: () -> Unit) {
    val ch = c.chapters.firstOrNull()
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Pad, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 60.dp, height = 88.dp).clip(RoundedCornerShape(8.dp)).background(Ui.Card)) {
            CoverImage(c.coverUrl, c.title, Modifier.fillMaxSize(), 180, 264)
            TypeBadge(displayType(c), Modifier.align(Alignment.TopStart).padding(3.dp), small = true)
        }
        Column(Modifier.weight(1f).height(88.dp).padding(start = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(c.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ComicStats(c, compact = true)
                if (isFresh(ch?.timeAgo)) NewBadge(small = true)
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Ui.Card).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(latestChapterText(c), fontSize = 14.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(latestAgoText(c), fontSize = 12.sp, color = Ui.Text2, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RankRow(rank: Int, c: Comic, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Text("$rank", fontSize = 31.sp, fontWeight = FontWeight.Bold, color = if (rank <= 3) Ui.Accent else Ui.Rank, textAlign = TextAlign.Center, modifier = Modifier.width(40.dp))
        Box(Modifier.size(width = 50.dp, height = 74.dp).clip(RoundedCornerShape(8.dp)).background(Ui.Card)) {
            CoverImage(c.coverUrl, c.title, Modifier.fillMaxSize(), 140, 210)
            TypeBadge(displayType(c), Modifier.align(Alignment.TopStart).padding(2.dp), small = true)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(c.title, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            ComicStats(c, compact = true)
            Text(latestLine(c), fontSize = 12.sp, color = Ui.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CardRow(comics: List<Comic>, width: Dp, onClick: (Comic) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Pad).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(Gap), verticalAlignment = Alignment.Top) {
        comics.forEach { c -> ComicCard(c, { onClick(c) }, Modifier.width(width)) }
    }
}
