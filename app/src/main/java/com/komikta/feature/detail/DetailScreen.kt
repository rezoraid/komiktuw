package com.komikta.feature.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.komikta.domain.model.Chapter
import com.komikta.ui.components.CoverImage
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.DetailShimmerLoading
import java.text.NumberFormat
import java.util.Locale

private val Muted = Color(0xFF9E9EA3)
private val SoftText = Color(0xFFB8B8BD)
private val Panel = Color(0xFF1A1A1C)
private val PanelBorder = Color(0x26FFFFFF)

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

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            uiState.isLoading && detail == null -> Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 64.dp)) {
                DetailShimmerLoading()
            }
            uiState.errorMessage != null && detail == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage ?: "Error", color = SoftText)
                        TextButton(onClick = { viewModel.loadDetail() }) { Text("Retry", color = Color.White) }
                    }
                }
            }
            detail != null -> {
                val ordered = if (uiState.chaptersNewestFirst) detail.chapters else detail.chapters.reversed()
                val visible = if (query.isBlank()) ordered else ordered.filter { chapterName(it).contains(query.trim(), ignoreCase = true) }
                val startChapter = detail.chapters.lastOrNull()

                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 128.dp)
                ) {
                    item {
                        Box(Modifier.fillMaxWidth().clipToBounds()) {
                            Box(Modifier.matchParentSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(detail.coverUrl)
                                        .size(Size(32, 44))
                                        .crossfade(false)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    filterQuality = FilterQuality.Medium,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(
                                            0f to Color.Black.copy(alpha = 0.45f),
                                            0.45f to Color.Black.copy(alpha = 0.6f),
                                            1f to Color.Black
                                        )
                                    )
                                )
                            }

                            Column(Modifier.fillMaxWidth()) {
                                Spacer(Modifier.height(230.dp))
                                Row(Modifier.padding(horizontal = 14.dp)) {
                                    CoverImage(
                                        url = detail.coverUrl,
                                        contentDescription = detail.title,
                                        widthPx = 336,
                                        heightPx = 450,
                                        modifier = Modifier
                                            .width(112.dp)
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Panel)
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (!detail.status.isNullOrBlank()) Pill(detail.status!!)
                                            if (!detail.year.isNullOrBlank()) Pill(detail.year!!)
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = detail.title,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontSize = 22.sp,
                                            lineHeight = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                Column(
                                    Modifier.padding(horizontal = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    detail.alternativeTitles.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, color = Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    detail.author?.takeIf { it.isNotBlank() }?.let { MetaRow(AppIcons.person, it) }
                                    detail.artist?.takeIf { it.isNotBlank() }?.let { MetaRow(AppIcons.palette, "Artist: $it") }
                                    detail.type?.takeIf { it.isNotBlank() }?.let { MetaRow(AppIcons.doc, "Format: $it") }
                                    detail.views?.let { MetaRow(AppIcons.eye, "Views: ${formatViews(it)}") }
                                    MetaRow(
                                        AppIcons.doc,
                                        "${detail.totalChapters ?: detail.chapters.size} chapter" +
                                            (detail.status?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")
                                    )
                                }
                                Spacer(Modifier.height(18.dp))
                            }
                        }
                    }

                    if (!detail.synopsis.isNullOrBlank()) {
                        item {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { synopsisExpanded = !synopsisExpanded }
                                    .padding(horizontal = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = detail.synopsis!!,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontSize = 15.sp,
                                    lineHeight = 26.sp,
                                    color = SoftText,
                                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 5,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    if (synopsisExpanded) AppIcons.chevronUp else AppIcons.chevronDown,
                                    contentDescription = null,
                                    tint = Muted,
                                    modifier = Modifier.padding(vertical = 10.dp).size(22.dp)
                                )
                            }
                        }
                    }

                    if (detail.genres.isNotEmpty()) {
                        item {
                            FlowRow(
                                Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                detail.genres.forEach { genre ->
                                    Surface(shape = RoundedCornerShape(20.dp), color = Panel) {
                                        Text(
                                            text = genre,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SoftText,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, top = 24.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Chapters", style = MaterialTheme.typography.titleLarge, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text("${detail.chapters.size}", style = MaterialTheme.typography.bodyLarge, color = Muted)
                        }
                    }

                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .padding(bottom = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(Panel)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(AppIcons.search, null, tint = Muted, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (query.isEmpty()) {
                                        Text("Cari chapter…", style = MaterialTheme.typography.bodyLarge, fontSize = 16.sp, color = Muted)
                                    }
                                    BasicTextField(
                                        value = query,
                                        onValueChange = { query = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = Color.White),
                                        cursorBrush = SolidColor(Color.White),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            RoundIconButton(
                                icon = if (uiState.chaptersNewestFirst) AppIcons.chevronDown else AppIcons.chevronUp,
                                description = "Sort",
                                size = 52.dp,
                                container = Panel,
                                onClick = { viewModel.toggleChapterOrder() }
                            )
                            RoundIconButton(
                                icon = AppIcons.download,
                                description = "Download",
                                size = 52.dp,
                                container = Panel,
                                onClick = {}
                            )
                        }
                    }

                    items(visible, key = { it.slug }) { chapter ->
                        ChapterRow(
                            chapter = chapter,
                            isRead = chapter.slug in readChapters,
                            onClick = { onChapterClick(detail.source, detail.slug, chapter.slug) }
                        )
                    }
                }

                if (startChapter != null) {
                    Surface(
                        onClick = { onChapterClick(detail.source, detail.slug, startChapter.slug) },
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF161618),
                        border = BorderStroke(1.dp, PanelBorder),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(AppIcons.play, null, tint = SoftText, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Mulai Membaca", style = MaterialTheme.typography.titleLarge, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text(chapterName(startChapter), style = MaterialTheme.typography.bodyLarge, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(AppIcons.chevronRight, null, tint = SoftText, modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RoundIconButton(
                icon = AppIcons.chevronLeft,
                description = "Back",
                size = 46.dp,
                container = Color(0xB3141416),
                border = PanelBorder,
                onClick = onBack
            )
            if (detail != null) {
                RoundIconButton(
                    icon = AppIcons.bookmark(isBookmarked),
                    description = "Bookmark",
                    size = 46.dp,
                    container = Color(0xB3141416),
                    border = PanelBorder,
                    onClick = { viewModel.toggleBookmark() }
                )
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    description: String,
    size: androidx.compose.ui.unit.Dp,
    container: Color,
    border: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        border = border?.let { BorderStroke(1.dp, it) },
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun Pill(text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0x40FFFFFF)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = SoftText,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MetaRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, fontSize = 15.sp, color = SoftText, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ChapterRow(chapter: Chapter, isRead: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (isRead) 0.4f else 1f)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = chapterName(chapter),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFD8D8DC),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!chapter.timeAgo.isNullOrBlank()) {
                Text(chapter.timeAgo!!, style = MaterialTheme.typography.bodyLarge, color = Muted)
            }
        }
        Icon(AppIcons.download, "Download", tint = Muted, modifier = Modifier.padding(horizontal = 12.dp).size(24.dp))
    }
}

private fun chapterName(chapter: Chapter): String = chapter.title ?: chapter.number ?: chapter.slug

private fun formatViews(v: Double): String = NumberFormat.getIntegerInstance(Locale("id", "ID")).format(v.toLong())
