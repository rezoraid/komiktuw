package com.komikta.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.domain.model.Comic
import com.komikta.ui.theme.Ui

/**
 * Satu-satunya tempat yang menulis rating, views, genre, dan chapter terbaru sebuah komik.
 * Home dan Explore (semua sumber) memakai ini, jadi isi dan formatnya selalu sama.
 * Data yang memang tidak ada ditulis "–" supaya posisi tiap baris tetap sejajar antar-kartu.
 */
const val NoData = "–"

fun genreText(comic: Comic, max: Int = 2): String = comic.genres.take(max).joinToString(", ").ifBlank { NoData }

/** "Ch. 12" untuk chapter terbaru; judul chapter kalau tidak ada nomor; "–" kalau tidak ada. */
fun latestChapterText(comic: Comic): String {
    val ch = comic.chapters.firstOrNull() ?: return NoData
    return chapterShort(ch) ?: ch.title?.takeIf { it.isNotBlank() } ?: NoData
}

/** "23 jam lalu" / "3 hari lalu" untuk chapter terbaru; "–" kalau tidak ada. */
fun latestAgoText(comic: Comic, nowMs: Long = System.currentTimeMillis()): String =
    chapterAgo(comic.chapters.firstOrNull()?.timeAgo, nowMs) ?: NoData

@Composable
fun RatingText(value: Double?, compact: Boolean = false) {
    val fontSize = if (compact) 11.sp else 13.sp
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.star, null, tint = if (value != null) Ui.Star else Ui.Text3, modifier = Modifier.size(if (compact) 11.dp else 14.dp))
        Text(
            value?.takeIf { it > 0 }?.let(::fmtRating) ?: NoData, fontSize = fontSize, fontWeight = FontWeight.Medium,
            color = if (value != null) Color.White else Ui.Text3, maxLines = 1, modifier = Modifier.padding(start = if (compact) 2.dp else 4.dp)
        )
    }
}

@Composable
fun ViewsText(value: Double?, compact: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.eye, null, tint = Ui.Text2, modifier = Modifier.size(if (compact) 11.dp else 16.dp))
        Text(
            value?.takeIf { it > 0 }?.let(::fmtViews) ?: NoData, fontSize = if (compact) 11.sp else 13.sp,
            color = if (value != null) Ui.Text2 else Ui.Text3, maxLines = 1, modifier = Modifier.padding(start = if (compact) 2.dp else 4.dp)
        )
    }
}

/** Rating + views dalam satu baris, selalu keduanya. */
@Composable
fun ComicStats(comic: Comic, modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)) {
        RatingText(comic.rating, compact)
        ViewsText(comic.views, compact)
    }
}
