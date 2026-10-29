package com.komikta.feature.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.komikta.R
import com.komikta.domain.model.Comic
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.CoverImage
import com.komikta.ui.components.HomeShimmerLoading
import kotlinx.coroutines.delay

private val CardBg = Color(0xFF111111)
private val CardBorder = Color(0xFF1F1F1F)
private val Muted = Color(0xFF9A9A9A)
private val Gold = Color(0xFFFFC107)

private fun fmtRating(r: Double): String = String.format(java.util.Locale.US, "%.1f", r)

@Composable
fun HomeScreen(
    onComicClick: (source: String, slug: String) -> Unit = { _, _ -> },
    onSearchClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val continueReading by viewModel.continueReading.collectAsState()

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
            LazyColumn(
                Modifier.fillMaxSize().background(Color(0xFF000000)),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Header + search bar
                item {
                    Column(Modifier.padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_logo),
                                contentDescription = "Komikta",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Komikta",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        SearchPill(onClick = onSearchClick)
                    }
                }

                // Featured carousel
                if (uiState.popularComics.isNotEmpty()) {
                    item {
                        FeaturedCarousel(
                            comics = uiState.popularComics.take(5),
                            onClick = { onComicClick(it.source, it.slug) }
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }

                // Continue reading
                if (continueReading.isNotEmpty()) {
                    item { SectionHeader("Continue reading", "History") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(continueReading, key = { it.id }) { item ->
                                ContinueCard(
                                    title = item.comicTitle,
                                    chapter = item.lastChapterNumber ?: item.lastChapterSlug ?: "",
                                    coverUrl = item.coverUrl,
                                    onClick = { onComicClick(item.source, item.comicSlug) }
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }

                // Recommended (ranked)
                if (uiState.popularComics.size > 1) {
                    item { SectionHeader("Recommended", "Top picks") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val ranked = uiState.popularComics.take(10)
                            items(ranked.size, key = { "${ranked[it].source}:${ranked[it].slug}" }) { index ->
                                RankedCard(
                                    rank = index + 1,
                                    comic = ranked[index],
                                    onClick = { onComicClick(ranked[index].source, ranked[index].slug) },
                                    modifier = Modifier.width(132.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }

                // Latest updates
                item { SectionHeader("Latest updates", null) }

                items(uiState.latestComics.take(20), key = { "${it.source}:${it.slug}" }) { comic ->
                    LatestItem(
                        comic = comic,
                        onClick = { onComicClick(comic.source, comic.slug) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(Color(0xFF151515))
            .border(1.dp, Color(0xFF262626), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = "Search", tint = Muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text("Cari komik...", style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

@Composable
private fun FeaturedCarousel(comics: List<Comic>, onClick: (Comic) -> Unit) {
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

    Column {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            FeaturedCard(comic = comics[page], onClick = { onClick(comics[page]) })
        }
        if (comics.size > 1) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(comics.size) { i ->
                    val selected = pagerState.currentPage == i
                    val w by animateDpAsState(if (selected) 20.dp else 6.dp, label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(w)
                            .clip(CircleShape)
                            .background(if (selected) Color.White else Color(0xFF3A3A3A))
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedCard(comic: Comic, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(shape)
            .background(CardBg)
            .clickable(onClick = onClick)
    ) {
        CoverImage(
            url = comic.coverUrl,
            contentDescription = comic.title,
            widthPx = 720,
            heightPx = 460,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.4f to Color.Black.copy(0.35f),
                    1f to Color.Black.copy(0.94f)
                )
            )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!comic.type.isNullOrBlank()) {
                    TypeChip(comic.type!!)
                    Spacer(Modifier.width(8.dp))
                }
                RatingChip(comic.rating)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = comic.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (comic.genres.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = comic.genres.take(3).joinToString(" • "),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFCFCFCF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Read now", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ContinueCard(title: String, chapter: String, coverUrl: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .width(268.dp)
            .clip(shape)
            .background(CardBg)
            .border(1.dp, CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(
            url = coverUrl,
            contentDescription = title,
            widthPx = 180,
            heightPx = 250,
            modifier = Modifier
                .size(width = 56.dp, height = 78.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1C1C1C))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                chapter,
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Lanjut", tint = Color.Black, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun RankedCard(rank: Int, comic: Comic, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(shape)
                .background(Color(0xFF171717))
        ) {
            CoverImage(
                url = comic.coverUrl,
                contentDescription = comic.title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(0.8f)
                    )
                )
            )
            Text(
                text = "$rank",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 2.dp)
            )
            RatingChip(
                comic.rating,
                Modifier.align(Alignment.TopEnd).padding(6.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = comic.title,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, action: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
            )
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (action != null) {
            Text(action, style = MaterialTheme.typography.labelLarge, color = Muted)
        }
    }
}

@Composable
private fun TypeChip(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun RatingChip(rating: Double?, modifier: Modifier = Modifier) {
    if (rating == null) return
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text(fmtRating(rating), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

@Composable
private fun LatestItem(comic: Comic, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(shape)
            .background(CardBg)
            .border(1.dp, CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(
            url = comic.coverUrl,
            contentDescription = comic.title,
            widthPx = 210,
            heightPx = 288,
            modifier = Modifier
                .size(width = 70.dp, height = 96.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF171717))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = comic.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (comic.genres.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = comic.genres.take(3).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!comic.type.isNullOrBlank()) {
                    TypeChip(comic.type!!)
                    Spacer(Modifier.width(8.dp))
                }
                if (comic.rating != null) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        fmtRating(comic.rating!!),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFDDDDDD)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (!comic.status.isNullOrBlank()) {
                    Text(
                        comic.status!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        Icon(AppIcons.chevronRight, contentDescription = null, tint = Color(0xFF5A5A5A), modifier = Modifier.size(20.dp))
    }
}
