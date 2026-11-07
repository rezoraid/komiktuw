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
 * Home dan Explore memakai ini, jadi isi dan formatnya selalu sama untuk Manta, Shinigami, dan Lumos.
 *
 * Aturan: data yang tidak tersedia TIDAK ditampilkan (fungsi mengembalikan null / composable tidak menggambar apa pun).
 * Tidak ada tanda "-" atau teks pengganti, dan tidak ada data karangan.
 */
fun genreText(comic: Comic, max: Int = 2): String? =
    comic.genres.take(max).joinToString(", ").takeIf { it.isNotBlank() }

/** "Ch. 12" untuk chapter terbaru; judul chapter kalau tidak ada nomor; null kalau tidak ada. */
fun latestChapterText(comic: Comic): String? {
    val ch = comic.chapters.firstOrNull() ?: return null
    return chapterShort(ch) ?: ch.title?.takeIf { it.isNotBlank() }
}

/** "23 jam lalu" / "3 hari lalu" untuk chapter terbaru; null kalau tidak ada. */
fun latestAgoText(comic: Comic, nowMs: Long = System.currentTimeMillis()): String? =
    chapterAgo(comic.chapters.firstOrNull()?.timeAgo, nowMs)

/** "Ch. 12 · 3 jam lalu", salah satunya saja kalau hanya satu yang ada, null kalau keduanya kosong. */
fun latestLine(comic: Comic): String? =
    listOfNotNull(latestChapterText(comic), latestAgoText(comic)).joinToString(" · ").ifEmpty { null }

@Composable
fun RatingText(value: Double?, compact: Boolean = false) {
    val rating = value?.takeIf { it > 0 } ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.star, null, tint = Ui.Star, modifier = Modifier.size(if (compact) 11.dp else 14.dp))
        Text(
            fmtRating(rating), fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.Medium,
            color = Color.White, maxLines = 1, modifier = Modifier.padding(start = if (compact) 2.dp else 4.dp)
        )
    }
}

@Composable
fun ViewsText(count: Long?, compact: Boolean = false) {
    val views = count?.takeIf { it > 0 } ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.eye, null, tint = Ui.Text2, modifier = Modifier.size(if (compact) 11.dp else 16.dp))
        Text(
            fmtViews(views.toDouble()), fontSize = if (compact) 11.sp else 13.sp,
            color = Ui.Text2, maxLines = 1, modifier = Modifier.padding(start = if (compact) 2.dp else 4.dp)
        )
    }
}

fun hasStats(comic: Comic): Boolean = (comic.rating ?: 0.0) > 0.0 || (comic.viewCount ?: 0L) > 0L

/** Rating dan views dalam satu baris; hanya yang tersedia. Tidak menggambar apa pun kalau keduanya kosong. */
@Composable
fun ComicStats(comic: Comic, modifier: Modifier = Modifier, compact: Boolean = false) {
    if (!hasStats(comic)) return
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)) {
        RatingText(comic.rating, compact)
        ViewsText(comic.viewCount, compact)
    }
}
