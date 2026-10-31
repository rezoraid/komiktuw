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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komikta.domain.model.Chapter
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.Chip
import com.komikta.ui.components.CoverImage
import com.komikta.ui.components.DetailSkeleton
import com.komikta.ui.theme.Ui
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

private val Soft = Color(0xFFC4C4C8)
private val ReadColor = Color(0xFF6E6E6E)
private val IdLocale = Locale("id", "ID")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onChapterClick: (source: String, comicSlug: String, chapterSlug: String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val readChapters by viewModel.readChapters.collectAsStateWithLifecycle()
    val detail = uiState.detail
    var query by remember { mutableStateOf("") }
    var synopsisExpanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ui.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(AppIcons.chevronLeft, "Back", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            if (detail != null) {
                IconButton(onClick = viewModel::toggleBookmark) {
                    Icon(AppIcons.bookmark(isBookmarked), "Bookmark", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }

        when {
            uiState.isLoading && detail == null -> DetailSkeleton()
            uiState.errorMessage != null && detail == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage ?: "Error", color = Ui.Text2)
                        TextButton(onClick = viewModel::loadDetail) { Text("Retry", color = Color.White) }
                    }
                }
            }
            detail != null -> {
                val ordered = remember(detail.chapters, uiState.chaptersNewestFirst) {
                    if (uiState.chaptersNewestFirst) detail.chapters else detail.chapters.reversed()
                }
                val visible = remember(ordered, query) {
                    if (query.isBlank()) ordered else ordered.filter { chapterName(it).contains(query.trim(), ignoreCase = true) }
                }

                LazyColumn(Modifier.weight(1f)) {
                    item(key = "info", contentType = "info") {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            CoverImage(
                                url = detail.coverUrl,
                                contentDescription = detail.title,
                                widthPx = 300,
                                heightPx = 420,
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Ui.Card)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = detail.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontSize = 20.sp,
                                    lineHeight = 26.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                detail.alternativeTitles.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, fontSize = 12.sp, color = Ui.Text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                val line = listOfNotNull(
                                    detail.status?.takeIf { it.isNotBlank() },
                                    detail.type?.takeIf { it.isNotBlank() },
                                    detail.year?.takeIf { it.isNotBlank() }
                                ).joinToString(" · ")
                                if (line.isNotEmpty()) Text(line, fontSize = 12.sp, color = Soft)
                            }
                        }
                    }

                    item(key = "meta", contentType = "meta") {
                        Column(
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            detail.author?.takeIf { it.isNotBlank() }?.let { MetaLine("Author", it) }
                            detail.artist?.takeIf { it.isNotBlank() }?.let { MetaLine("Artist", it) }
                            detail.views?.let { MetaLine("Views", NumberFormat.getIntegerInstance(IdLocale).format(it.toLong())) }
                            MetaLine("Chapter", "${detail.totalChapters ?: detail.chapters.size}")
                        }
                    }

                    if (!detail.synopsis.isNullOrBlank()) {
                        item(key = "synopsis", contentType = "synopsis") {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { synopsisExpanded = !synopsisExpanded }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = detail.synopsis.orEmpty(),
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = Soft,
                                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (synopsisExpanded) "Tutup" else "Selengkapnya",
                                    fontSize = 12.sp,
                                    color = Ui.Text2,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    if (detail.genres.isNotEmpty()) {
                        item(key = "genres", contentType = "genres") {
                            FlowRow(
                                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                detail.genres.forEach { genre ->
                                    Text(
                                        text = genre,
                                        fontSize = 12.sp,
                                        color = Soft,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Ui.Chip)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    item(key = "chapters-header", contentType = "chapters-header") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Ui.Card)
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(AppIcons.search, null, tint = Ui.Text2, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (query.isEmpty()) {
                                        Text("Cari chapter (${detail.chapters.size})", fontSize = 14.sp, color = Ui.Text3, maxLines = 1)
                                    }
                                    BasicTextField(
                                        value = query,
                                        onValueChange = { query = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge,
                                        cursorBrush = SolidColor(Color.White),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Chip(
                                text = if (uiState.chaptersNewestFirst) "Terbaru" else "Terlama",
                                selected = false,
                                onClick = viewModel::toggleChapterOrder
                            )
                        }
                    }

                    items(visible, key = { it.slug }, contentType = { "chapter" }) { chapter ->
                        ChapterRow(
                            chapter = chapter,
                            isRead = chapter.slug in readChapters,
                            onClick = { onChapterClick(detail.source, detail.slug, chapter.slug) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    Row {
        Text(label, fontSize = 13.sp, color = Ui.Text3, modifier = Modifier.width(64.dp))
        Text(value, fontSize = 13.sp, color = Soft, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ChapterRow(chapter: Chapter, isRead: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = chapterName(chapter),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isRead) ReadColor else Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val ago = chapter.timeAgo
        if (!ago.isNullOrBlank()) {
            Text(ago, fontSize = 12.sp, color = Ui.Text3, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

private fun chapterName(chapter: Chapter): String = chapter.title ?: chapter.number ?: chapter.slug
