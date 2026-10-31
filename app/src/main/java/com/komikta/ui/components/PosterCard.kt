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

private val CardShape = RoundedCornerShape(8.dp)
private val TagShape = RoundedCornerShape(5.dp)

@Composable
fun PosterCard(
    title: String,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    rating: String? = null,
    meta: String? = null
) {
    Column(modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(CardShape)
                .background(Ui.Card)
        ) {
            CoverImage(url = coverUrl, contentDescription = title, modifier = Modifier.fillMaxSize())
            if (badge != null) Tag(badge, Modifier.align(Alignment.TopStart).padding(5.dp))
            if (rating != null) Tag(rating, Modifier.align(Alignment.TopEnd).padding(5.dp))
        }
        Text(
            text = title,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (meta != null) {
            Text(
                text = meta,
                fontSize = 11.sp,
                color = Ui.Text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun Tag(text: String, modifier: Modifier) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        maxLines = 1,
        modifier = modifier
            .background(Ui.Badge, TagShape)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

@Composable
fun ComicCard(
    comic: Comic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badge = remember(comic) {
        comic.type?.trim()?.takeIf { it.isNotEmpty() }?.replaceFirstChar { it.uppercase() }
    }
    val rating = remember(comic) {
        comic.rating?.takeIf { it > 0.0 }?.let { "★ ${fmtRating(it)}" }
    }
    val meta = remember(comic) {
        comic.chapters.firstOrNull()?.let(::chapterLong) ?: comic.status?.takeIf { it.isNotBlank() }
    }
    PosterCard(
        title = comic.title,
        coverUrl = comic.coverUrl,
        onClick = onClick,
        modifier = modifier,
        badge = badge,
        rating = rating,
        meta = meta
    )
}
