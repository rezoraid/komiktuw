package com.komikta.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.komikta.R
import com.komikta.domain.model.Comic
import com.komikta.ui.components.ComicCard
import com.komikta.ui.components.CoverImage
import com.komikta.ui.components.HomeShimmerLoading

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
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Header
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = "Komikta",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Komikta",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onSearchClick() }
                        )
                    }
                }

                // Featured banner
                if (uiState.popularComics.isNotEmpty()) {
                    val featured = uiState.popularComics.first()
                    item {
                        FeaturedBanner(
                            comic = featured,
                            onClick = { onComicClick(featured.source, featured.slug) }
                        )
                        Spacer(Modifier.height(20.dp))
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
                                Column(
                                    Modifier
                                        .width(140.dp)
                                        .clickable { onComicClick(item.source, item.comicSlug) }
                                ) {
                                    CoverImage(
                                        url = item.coverUrl,
                                        contentDescription = item.comicTitle,
                                        widthPx = 420,
                                        heightPx = 270,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(90.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        item.comicTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        item.lastChapterNumber ?: item.lastChapterSlug ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF999999),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Popular / Recommended
                if (uiState.popularComics.size > 1) {
                    item {
                        SectionHeader("Recommended", "See all")
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.popularComics.drop(1).take(10), key = { "${it.source}:${it.slug}" }) { comic ->
                                ComicCard(
                                    comic = comic,
                                    onClick = { onComicClick(comic.source, comic.slug) },
                                    modifier = Modifier.width(130.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
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
private fun FeaturedBanner(comic: Comic, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        CoverImage(
            url = comic.coverUrl,
            contentDescription = null,
            widthPx = 720,
            heightPx = 360,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(Color.Black.copy(0.85f), Color.Black.copy(0.3f), Color.Transparent)
                )
            )
        )
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(20.dp)
                .fillMaxWidth(0.6f)
        ) {
            if (!comic.type.isNullOrBlank()) {
                Text(
                    text = comic.type!!.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFFFFF)
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = comic.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text("Read now", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (action != null) {
            Text(action, style = MaterialTheme.typography.labelLarge, color = Color(0xFFFFFFFF))
        }
    }
}

@Composable
private fun LatestItem(comic: Comic, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(
            url = comic.coverUrl,
            contentDescription = comic.title,
            widthPx = 192,
            heightPx = 264,
            modifier = Modifier
                .size(width = 64.dp, height = 88.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF171717))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!comic.type.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFFFFFF).copy(0.2f)
                    ) {
                        Text(
                            text = comic.type!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFFFFF),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = listOfNotNull(
                    comic.rating?.let { "★ $it" },
                    comic.status
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF999999)
            )
        }
    }
}
