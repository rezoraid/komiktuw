package com.komikta.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.komikta.domain.model.Chapter
import com.komikta.ui.components.DetailShimmerLoading

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onChapterClick: (source: String, comicSlug: String, chapterSlug: String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val detail = uiState.detail

    Scaffold(
        containerColor = Color(0xFF0D0D0F),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.Black.copy(0.4f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding)) { DetailShimmerLoading() }
            uiState.errorMessage != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage ?: "Error")
                        TextButton(onClick = { viewModel.loadDetail() }) { Text("Retry") }
                    }
                }
            }
            detail != null -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    // Banner background
                    item {
                        Box(Modifier.fillMaxWidth().height(260.dp)) {
                            AsyncImage(
                                model = detail.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xFF0D0D0F))
                                    )
                                )
                            )
                            // Cover + title overlay
                            Row(
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                AsyncImage(
                                    model = detail.coverUrl,
                                    contentDescription = detail.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(155.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = detail.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (!detail.type.isNullOrBlank()) {
                                            Chip(detail.type!!)
                                        }
                                        if (!detail.status.isNullOrBlank()) {
                                            Chip(detail.status!!, Color(0xFF22C55E))
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    if (!detail.author.isNullOrBlank()) {
                                        Text(
                                            text = detail.author!!,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFA1A1AA)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Stats
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem("★ ${detail.rating ?: "-"}", "Rating")
                            StatItem("${detail.totalChapters ?: detail.chapters.size}", "Chapters")
                            StatItem(detail.views?.let { formatViews(it) } ?: "-", "Views")
                        }
                    }

                    // Start Reading button
                    item {
                        val firstChapter = detail.chapters.lastOrNull() // usually oldest first or reverse
                        val startChapter = detail.chapters.firstOrNull()
                        Button(
                            onClick = {
                                startChapter?.let {
                                    onChapterClick(detail.source, detail.slug, it.slug)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start Reading", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Genres
                    if (detail.genres.isNotEmpty()) {
                        item {
                            FlowRow(
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                detail.genres.forEach { genre ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFF1E1E24)
                                    ) {
                                        Text(
                                            text = genre,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Synopsis
                    if (!detail.synopsis.isNullOrBlank()) {
                        item {
                            Text(
                                text = detail.synopsis!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD4D4D8),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Chapters header
                    item {
                        Text(
                            text = "Chapters · ${detail.chapters.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }

                    items(detail.chapters) { chapter ->
                        ChapterRow(
                            chapter = chapter,
                            onClick = { onChapterClick(detail.source, detail.slug, chapter.slug) }
                        )
                    }

                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color = Color(0xFF8B5CF6)) {
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.2f)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))
    }
}

@Composable
private fun ChapterRow(chapter: Chapter, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = chapter.title ?: chapter.number ?: chapter.slug,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (!chapter.timeAgo.isNullOrBlank()) {
            Text(
                text = chapter.timeAgo!!,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF71717A)
            )
        }
    }
    HorizontalDivider(color = Color(0xFF1E1E24), thickness = 0.5.dp)
}

private fun formatViews(v: Double): String {
    return when {
        v >= 1_000_000 -> String.format("%.1fM", v / 1_000_000)
        v >= 1_000 -> String.format("%.1fK", v / 1_000)
        else -> v.toInt().toString()
    }
}
