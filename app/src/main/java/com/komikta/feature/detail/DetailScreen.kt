package com.komikta.feature.detail

import com.komikta.ui.system.stableStatusBarsPadding
import com.komikta.ui.system.stableNavigationBarsPadding
import com.komikta.ui.components.ErrorState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import com.komikta.ui.components.fmtRating
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.komikta.data.local.SourcePrefs
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
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
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
import com.komikta.domain.model.ComicDetail
import com.komikta.domain.model.LanguageVariant
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.Chip
import com.komikta.ui.components.CoverImage
import com.komikta.ui.components.DetailSkeleton
import com.komikta.ui.components.SkeletonBlock
import com.komikta.ui.components.shimmerBrush
import com.komikta.ui.theme.Ui
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

private val Soft = Color(0xFFC3CBCE)
private val ReadColor = Color(0xFF6F7A80)
private val IdLocale = Locale("id", "ID")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onChapterClick: (source: String, comicSlug: String, chapterSlug: String) -> Unit,
    onCommentsClick: (source: String, slug: String) -> Unit,
    onLanguageChange: (source: String, slug: String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val readChapters by viewModel.readChapters.collectAsStateWithLifecycle()
    val chapterComments by viewModel.chapterComments.collectAsStateWithLifecycle()
    val like by viewModel.like.collectAsStateWithLifecycle()
    val likeMessage by viewModel.likeMessage.collectAsStateWithLifecycle()
    LaunchedEffect(likeMessage) {
        if (likeMessage != null) {
            delay(2500)
            viewModel.clearLikeMessage()
        }
    }
    LaunchedEffect(Unit) { viewModel.loadChapterComments() }
    val detail = uiState.detail
    var query by remember { mutableStateOf("") }
    var synopsisExpanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ui.Bg)
            .stableStatusBarsPadding()
            .stableNavigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(AppIcons.chevronLeft, "Back", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            if (detail != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::toggleLike) {
                        Icon(AppIcons.heart(like.liked), "Suka", tint = if (like.liked) Color(0xFFE5484D) else Color.White, modifier = Modifier.size(24.dp))
                    }
                    if (like.count > 0) Text("${like.count}", fontSize = 13.sp, color = Ui.Text2, modifier = Modifier.padding(end = 6.dp))
                    IconButton(onClick = viewModel::toggleBookmark) {
                        Icon(AppIcons.bookmark(isBookmarked), "Bookmark", tint = if (isBookmarked) Ui.Accent else Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        likeMessage?.let { Text(it, fontSize = 13.sp, color = Ui.Text2, modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) }

        when {
            uiState.isLoading && detail == null -> {
                val preview = uiState.preview
                if (preview == null) DetailSkeleton() else {
                    // Sampul + judul tampil duluan dari daftar; chapter menyusul begitu detail lengkap tiba.
                    val early = remember(preview.chapters) { preview.chapters.distinctBy { it.slug } }
                    LazyColumn(Modifier.weight(1f)) {
                        item(key = "info", contentType = "info") { DetailHeader(preview) }
                        items(early, key = { it.slug }, contentType = { "chapter" }) { chapter ->
                            ChapterRow(
                                chapter = chapter,
                                isRead = chapter.slug in readChapters,
                                comments = 0,
                                onClick = { onChapterClick(preview.source, preview.slug, chapter.slug) }
                            )
                        }
                        item(key = "chapters-skeleton", contentType = "skeleton") { ChapterSkeletonRows() }
                    }
                }
            }
            uiState.errorMessage != null && detail == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorState(uiState.errorMessage ?: "Terjadi kesalahan", { viewModel.loadDetail(refresh = true) })
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
                        DetailHeader(detail)
                    }

                    // Hanya muncul kalau serial ini punya lebih dari satu bahasa (MANGA Plus).
                    if (detail.languages.size > 1) {
                        item(key = "language", contentType = "language") {
                            LanguageRow(detail.language, detail.languages) { variant ->
                                if (variant.slug != detail.slug) {
                                    viewModel.rememberLanguage(variant.code)
                                    onLanguageChange(detail.source, variant.slug)
                                }
                            }
                        }
                    }

                    if (detail.chapters.isNotEmpty()) {
                        item(key = "read", contentType = "read") {
                            val target = remember(detail.chapters, readChapters) {
                                val asc = detail.chapters.reversed()
                                asc.firstOrNull { it.slug !in readChapters } ?: asc.last()
                            }
                            val started = readChapters.isNotEmpty()
                            Text(
                                text = (if (started) "Lanjut baca · " else "Mulai baca · ") + chapterName(target),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Ui.OnAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Ui.AccentBrush)
                                    .clickable { onChapterClick(detail.source, detail.slug, target.slug) }
                                    .padding(horizontal = 14.dp, vertical = 13.dp)
                            )
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
                                    fontSize = 15.sp,
                                    lineHeight = 23.sp,
                                    color = Soft,
                                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (synopsisExpanded) "Tutup" else "Selengkapnya",
                                    fontSize = 13.sp,
                                    color = Ui.Accent,
                                    fontWeight = FontWeight.Medium,
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
                                        fontSize = 13.sp,
                                        color = Soft,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Ui.Chip)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    item(key = "comments", contentType = "comments") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Ui.Card)
                                .clickable { onCommentsClick(detail.source, detail.slug) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(AppIcons.comment, null, tint = Ui.Accent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Komentar", fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Icon(AppIcons.chevronRight, null, tint = Ui.Text2, modifier = Modifier.size(20.dp))
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
                                    .height(43.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Ui.Card)
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(AppIcons.search, null, tint = Ui.Text2, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (query.isEmpty()) {
                                        Text("Cari chapter (${detail.chapters.size})", fontSize = 15.sp, color = Ui.Text3, maxLines = 1)
                                    }
                                    BasicTextField(
                                        value = query,
                                        onValueChange = { query = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge,
                                        cursorBrush = SolidColor(Ui.Accent),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            IconButton(onClick = viewModel::toggleChapterOrder) {
                                Icon(
                                    AppIcons.sort,
                                    contentDescription = if (uiState.chaptersNewestFirst) "Urutkan chapter terlama" else "Urutkan chapter terbaru",
                                    tint = Ui.Text2,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }

                    items(visible, key = { it.slug }, contentType = { "chapter" }) { chapter ->
                        ChapterRow(
                            chapter = chapter,
                            isRead = chapter.slug in readChapters,
                            comments = chapterComments[chapter.slug] ?: 0,
                            onClick = { onChapterClick(detail.source, detail.slug, chapter.slug) }
                        )
                    }
                }
            }
        }
    }
}

/** Bahasa yang benar-benar tersedia untuk serial ini; memilih membuka versi bahasa itu (chapter-nya sendiri). */
@Composable
private fun LanguageRow(current: String?, options: List<LanguageVariant>, onSelect: (LanguageVariant) -> Unit) {
    val ordered = remember(options) {
        val order = SourcePrefs.LANGUAGES.map { it.code }
        options.sortedBy { order.indexOf(it.code).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }
    Column(Modifier.padding(top = 6.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bahasa", fontSize = 13.sp, color = Ui.Text2, modifier = Modifier.padding(horizontal = 14.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ordered.forEach { v -> Chip(SourcePrefs.languageLabel(v.code), current == v.code, { onSelect(v) }) }
        }
    }
}

@Composable
private fun DetailHeader(detail: ComicDetail) {
    Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
        CoverImage(
            url = detail.coverUrl,
            contentDescription = detail.title,
            widthPx = 330,
            heightPx = 462,
            modifier = Modifier
                .width(110.dp)
                .height(154.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Ui.Card)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = detail.title,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 21.sp,
                lineHeight = 27.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            detail.alternativeTitles.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 13.sp, color = Ui.Text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            val line = listOfNotNull(
                detail.status?.takeIf { it.isNotBlank() },
                detail.type?.takeIf { it.isNotBlank() },
                detail.year?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (line.isNotEmpty()) Text(line, fontSize = 13.sp, color = Soft)
            detail.rating?.takeIf { it > 0.0 }?.let {
                Text("★ ${fmtRating(it)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ui.Star)
            }
        }
    }
}

@Composable
private fun ChapterSkeletonRows() {
    val brush = shimmerBrush()
    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(6) { SkeletonBlock(Modifier.fillMaxWidth().height(44.dp), brush) }
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    Row {
        Text(label, fontSize = 14.sp, color = Ui.Text3, modifier = Modifier.width(64.dp))
        Text(value, fontSize = 14.sp, color = Soft, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ChapterRow(chapter: Chapter, isRead: Boolean, comments: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = chapterName(chapter),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isRead) ReadColor else Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val ago = chapter.timeAgo
            if (!ago.isNullOrBlank()) {
                Text(ago, fontSize = 13.sp, color = Ui.Text3, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (comments > 0) {
            Icon(AppIcons.comment, null, tint = Ui.Text3, modifier = Modifier.padding(start = 10.dp).size(14.dp))
            Text("$comments", fontSize = 13.sp, color = Ui.Text2, modifier = Modifier.padding(start = 4.dp))
        }
        if (!isRead) Box(Modifier.padding(start = 10.dp).size(8.dp).background(Ui.Accent, CircleShape))
    }
}

private fun chapterName(chapter: Chapter): String = chapter.title ?: chapter.number ?: chapter.slug
