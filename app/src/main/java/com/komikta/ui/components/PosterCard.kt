package com.komikta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.domain.model.Comic
import com.komikta.ui.theme.Ui

private val CoverShape = RoundedCornerShape(12.dp)

@Composable
fun TypeBadge(text: String, modifier: Modifier = Modifier, small: Boolean = false) {
    Text(
        text, fontSize = if (small) 10.sp else 12.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1,
        modifier = modifier.background(Ui.Badge, RoundedCornerShape(if (small) 5.dp else 7.dp)).padding(horizontal = if (small) 4.dp else 7.dp, vertical = if (small) 2.dp else 3.dp)
    )
}

@Composable
fun NewBadge(modifier: Modifier = Modifier, small: Boolean = false) {
    Text(
        "NEW", fontSize = if (small) 10.sp else 12.sp, fontWeight = FontWeight.Bold, color = Ui.OnAccent, maxLines = 1,
        modifier = modifier.background(Ui.Accent, RoundedCornerShape(if (small) 5.dp else 6.dp)).padding(horizontal = if (small) 5.dp else 7.dp, vertical = if (small) 1.dp else 3.dp)
    )
}

@Composable
fun PosterCard(
    title: String,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    isNew: Boolean = false,
    meta: String? = null
) {
    Column(modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.667f).clip(CoverShape).background(Ui.Card)) {
            CoverImage(url = coverUrl, contentDescription = title, modifier = Modifier.fillMaxSize())
            if (badge != null) TypeBadge(badge, Modifier.align(Alignment.TopStart).padding(5.dp), small = true)
            if (isNew) NewBadge(Modifier.align(Alignment.TopEnd).padding(5.dp), small = true)
        }
        Text(title, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        if (!meta.isNullOrBlank()) Text(meta, fontSize = 13.sp, color = Ui.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
fun ComicCard(comic: Comic, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val badge = remember(comic) { typeLabel(comic.type) }
    val first = remember(comic) { comic.chapters.firstOrNull() }
    val meta = remember(comic) { first?.let { chapterShort(it) ?: it.title?.takeIf { t -> t.isNotBlank() } } ?: statusLabel(comic.status) }
    PosterCard(comic.title, comic.coverUrl, onClick, modifier, badge, isFresh(first?.timeAgo), meta)
}
