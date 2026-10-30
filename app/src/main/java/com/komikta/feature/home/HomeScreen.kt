package com.komikta.feature.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.komikta.R
import com.komikta.data.local.entity.ReadingHistoryEntity
import com.komikta.domain.model.Chapter
import com.komikta.domain.model.Comic
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.CoverImage
import com.komikta.ui.components.HomeShimmerLoading
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

private val Pad = 14.dp
private val Gap = 10.dp
private val HeaderBg = Color(0xFF0C0C0E)
private val Line = Color(0xFF1B1B1F)
private val Muted = Color(0xFF9A9AA3)
private val CoverBg = Color(0xFF16161A)
private val ChipBg = Color(0xFF1E1F24)
private val ChipText = Color(0xFFD6D6DC)
private val Gold = Color(0xFFFFC107)
private val CardShape = RoundedCornerShape(14.dp)
private val IdLocale = Locale("id", "ID")

@Composable
fun HomeScreen(
    onComicClick: (source: String, slug: String) -> Unit = { _, _ -> },
    onReadClick: (source: String, comicSlug: String, chapterSlug: String) -> Unit = { _, _, _ -> },
    onSearchClick: () -> Unit = {},
    onContinueViewAll: () -> Unit = {},
    onUpdateViewAll: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.continueReading.collectAsState()

    when {
        uiState.isLoading && uiState.latestComics.isEmpty() -> HomeShimmerLoading()
        uiState.errorMessage != null && uiState.latestComics.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.errorMessage ?: "Error")
                    TextButton(onClick = { viewModel.loadHome() }) { Text("Retry") }
                }
            }
        }
        else -> {
            val hero = remember(uiState.popularComics) { uiState.popularComics.take(5) }
            val update = remember(uiState.latestComics) { uiState.latestComics.take(6) }
            val updateRows = remember(update) { update.chunked(3) }
            val forYou = remember(uiState.latestComics) {
                uiState.latestComics.drop(6).sortedByDescending { it.rating ?: 0.0 }.take(10)
            }
            val popular = remember(uiState.popularComics) { uiState.popularComics.take(10) }
            val now = remember { System.currentTimeMillis() }
            val cardWidth = (LocalConfiguration.current.screenWidthDp.dp - Pad * 2 - Gap * 2) / 3

            // "Baca": lanjut dari riwayat kalau ada, kalau tidak buka chapter terbaru, kalau tidak ada -> detail
            val onRead: (Comic) -> Unit = { comic ->
                val slug = history.firstOrNull { it.source == comic.source && it.comicSlug == comic.slug }?.lastChapterSlug
                    ?: comic.chapters.firstOrNull()?.slug
                if (slug.isNullOrBlank()) onComicClick(comic.source, comic.slug)
                else onReadClick(comic.source, comic.slug, slug)
            }

            LazyColumn(
                Modifier.fillMaxSize().background(Color.Black),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "header") { HomeHeader(onSearch = onSearchClick) }

                if (hero.isNotEmpty()) {
                    item(key = "hero") {
                        HeroCarousel(
                            comics = hero,
                            onDetail = { onComicClick(it.source, it.slug) },
                            onRead = onRead
                        )
                    }
                }

                if (history.isNotEmpty()) {
                    item(key = "continue-title") { SectionHeader("Continue reading", onContinueViewAll) }
                    item(key = "continue-row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Pad),
                            horizontalArrangement = Arrangement.spacedBy(Gap)
                        ) {
                            items(history, key = { it.id }) { h ->
                                ContinueCard(
                                    item = h,
                                    now = now,
                                    onClick = {
                                        val ch = h.lastChapterSlug
                                        if (ch.isNullOrBlank()) onComicClick(h.source, h.comicSlug)
                                        else onReadClick(h.source, h.comicSlug, ch)
                                    }
                                )
                            }
                        }
                    }
                }

                if (updateRows.isNotEmpty()) {
                    item(key = "update-title") { SectionHeader("Update", onUpdateViewAll) }
                    items(updateRows.size, key = { "update:${updateRows[it].first().source}:${updateRows[it].first().slug}" }) { i ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = Pad).padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(Gap),
                            verticalAlignment = Alignment.Top
                        ) {
                            updateRows[i].forEach { comic ->
                                PosterCard(comic, { onComicClick(comic.source, comic.slug) }, Modifier.width(cardWidth))
                            }
                        }
                    }
                }

                if (forYou.isNotEmpty()) {
                    item(key = "foryou-title") { SectionHeader("Untukmu", null) }
                    item(key = "foryou-row") { PosterRow(forYou, cardWidth, onComicClick) }
                }

                if (popular.isNotEmpty()) {
                    item(key = "popular-title") { SectionHeader("Populer minggu ini", null) }
                    item(key = "popular-row") { PosterRow(popular, cardWidth, onComicClick) }
                }
            }
        }
    }
}

// ───────────────────────── Header ─────────────────────────

@Composable
private fun HomeHeader(onSearch: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(HeaderBg)) {
        Row(
            Modifier.fillMaxWidth().padding(start = Pad, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = "Komikta",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Komikta",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearch) {
                Icon(AppIcons.search, "Cari", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = { }) {
                Icon(AppIcons.bell, "Notifikasi", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        HorizontalDivider(color = Line)
    }
}

// ───────────────────────── Hero ─────────────────────────

@Composable
private fun HeroCarousel(
    comics: List<Comic>,
    onDetail: (Comic) -> Unit,
    onRead: (Comic) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { comics.size })

    LaunchedEffect(comics.size) {
        if (comics.size < 2) return@LaunchedEffect
        while (true) {
            delay(5000)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % comics.size)
            }
        }
    }

    Box(Modifier.fillMaxWidth().height(236.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            HeroSlide(
                rank = page + 1,
                comic = comics[page],
                onDetail = { onDetail(comics[page]) },
                onRead = { onRead(comics[page]) }
            )
        }
        if (comics.size > 1) {
            PagerDots(
                pagerState, comics.size,
                Modifier.align(Alignment.BottomEnd).padding(end = Pad, bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun HeroSlide(rank: Int, comic: Comic, onDetail: () -> Unit, onRead: () -> Unit) {
    val latest = comic.chapters.firstOrNull()
    Box(Modifier.fillMaxSize().background(CoverBg).clickable(onClick = onDetail)) {
        CoverImage(
            url = comic.coverUrl,
            contentDescription = comic.title,
            widthPx = 720,
            heightPx = 440,
            modifier = Modifier.fillMaxSize()
        )
        // gelap di kiri (teks) + gelap di bawah (menyatu dengan background)
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(0.85f),
                    0.6f to Color.Black.copy(0.4f),
                    1f to Color.Transparent
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black)
            )
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = Pad, end = Pad, bottom = 30.dp)
        ) {
            Text(
                "#$rank Trending",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB8B8C0)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                comic.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp, lineHeight = 32.sp),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (comic.genres.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    comic.genres.take(3).joinToString(", "),
                    fontSize = 13.sp,
                    color = Color(0xFFC9C9D0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                comic.rating?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(fmtRating(it), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }
                latest?.let { ch ->
                    val text = listOfNotNull(chapterShort(ch), ch.timeAgo?.takeIf { it.isNotBlank() }).joinToString(" · ")
                    if (text.isNotEmpty()) Text(text, fontSize = 13.sp, color = Color(0xFFE0E0E5), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                }
                comic.views?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.eye, null, tint = Color(0xFFE0E0E5), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(formatViewsShort(it), fontSize = 13.sp, color = Color(0xFFE0E0E5), maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onRead)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Baca", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                }
                Box(
                    Modifier
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable(onClick = onDetail)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Detail", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PagerDots(pagerState: PagerState, count: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val selected = pagerState.currentPage == i
            val w by animateDpAsState(if (selected) 22.dp else 6.dp, label = "dot")
            Box(
                Modifier
                    .padding(start = 5.dp)
                    .height(6.dp)
                    .width(w)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else Color(0xFF5A5A62))
            )
        }
    }
}

// ───────────────────────── Section ─────────────────────────

@Composable
private fun SectionHeader(title: String, onAction: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(start = Pad, end = Pad - 6.dp, top = 24.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        if (onAction != null) {
            Text(
                "View All",
                fontSize = 14.sp,
                color = Muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PosterRow(comics: List<Comic>, cardWidth: androidx.compose.ui.unit.Dp, onComicClick: (String, String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Pad),
        horizontalArrangement = Arrangement.spacedBy(Gap),
        verticalAlignment = Alignment.Top
    ) {
        items(comics, key = { "${it.source}:${it.slug}" }) { comic ->
            PosterCard(comic, { onComicClick(comic.source, comic.slug) }, Modifier.width(cardWidth))
        }
    }
}

// ───────────────────────── Cards ─────────────────────────

@Composable
private fun ContinueCard(item: ReadingHistoryEntity, now: Long, onClick: () -> Unit) {
    Column(Modifier.width(126.dp).clip(CardShape).clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.75f).clip(CardShape).background(CoverBg)
        ) {
            CoverImage(
                url = item.coverUrl,
                contentDescription = item.comicTitle,
                widthPx = 300,
                heightPx = 400,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(0.9f))
                )
            )
            Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text(
                    historyChapterLabel(item),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color(0xFFD0D0D6), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Lanjutkan", fontSize = 12.sp, color = Color(0xFFD0D0D6))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            item.comicTitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(relativeTime(item.lastReadAt, now), fontSize = 12.sp, color = Muted, maxLines = 1)
    }
}

@Composable
private fun PosterCard(comic: Comic, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val latest = comic.chapters.firstOrNull()
    val chapterPill = latest?.let { chapterShort(it) } ?: comic.chapterCount?.let { "Ch. $it" }
    val chapterChip = latest?.let { chapterLong(it) }

    Column(modifier.clip(CardShape).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.75f).clip(CardShape).background(CoverBg)) {
            CoverImage(
                url = comic.coverUrl,
                contentDescription = comic.title,
                widthPx = 300,
                heightPx = 400,
                modifier = Modifier.fillMaxSize()
            )
            chapterPill?.let {
                Text(
                    it,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
            comic.rating?.let {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(fmtRating(it), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            comic.title,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        comic.author?.let { MetaRow(AppIcons.person, it) }
        comic.views?.let { MetaRow(AppIcons.eye, formatViewsFull(it)) }
        comic.type?.takeIf { it.isNotBlank() }?.let { MetaRow(AppIcons.doc, it) }
        chapterChip?.let {
            Text(
                it,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ChipText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ChipBg)
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun MetaRow(icon: ImageVector, text: String) {
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Muted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, fontSize = 12.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ───────────────────────── Helpers ─────────────────────────

private val LeadingChapter = Regex("""(?i)^\s*(chapter|ch\.?)\s*""")

private fun cleanNumber(raw: String?): String? =
    raw?.replace(LeadingChapter, "")?.trim()?.takeIf { it.isNotEmpty() }

private fun chapterShort(c: Chapter): String? = cleanNumber(c.number)?.let { "Ch. $it" }

private fun chapterLong(c: Chapter): String? {
    val name = cleanNumber(c.number)?.let { "Chapter $it" } ?: c.title?.takeIf { it.isNotBlank() } ?: return null
    val ago = c.timeAgo?.takeIf { it.isNotBlank() }
    return if (ago != null) "$name · $ago" else name
}

private fun historyChapterLabel(h: ReadingHistoryEntity): String =
    cleanNumber(h.lastChapterNumber)?.let { "Chapter $it" } ?: "Lanjutkan baca"

private fun fmtRating(r: Double): String = String.format(Locale.US, "%.1f", r)

private fun formatViewsFull(v: Double): String = NumberFormat.getIntegerInstance(IdLocale).format(v.toLong())

private fun formatViewsShort(v: Double): String = when {
    v >= 1_000_000 -> String.format(IdLocale, "%.1f jt", v / 1_000_000)
    v >= 1_000 -> String.format(IdLocale, "%.1f rb", v / 1_000)
    else -> v.toLong().toString()
}

private fun relativeTime(thenMs: Long, nowMs: Long): String {
    val s = ((nowMs - thenMs) / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "baru saja"
        s < 3_600 -> "${s / 60} menit lalu"
        s < 86_400 -> "${s / 3_600} jam lalu"
        s < 86_400 * 30 -> "${s / 86_400} hari lalu"
        else -> "${s / (86_400 * 30)} bulan lalu"
    }
}
