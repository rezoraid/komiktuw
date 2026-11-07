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
import com.komikta.domain.model.Chapter
import com.komikta.domain.model.Comic
import com.komikta.domain.model.ComicDetail
import com.komikta.ui.theme.Ui

/**
 * Satu-satunya tempat yang menulis rating, views, genre, jenis, status, author, dan chapter terbaru sebuah komik.
 * Home, Explore, Library, dan Detail memakai ini, jadi isi dan formatnya sama untuk Manta, Shinigami, dan Lumos.
 *
 * Aturan: setiap bagian SELALU punya isi. Server sudah melengkapi data (sinkronisasi, sumber lain, metadata luar);
 * di sini hanya jaring pengaman untuk data yang tetap kosong (data lama di cache, server versi lama, atau sumber
 * yang memang tidak punya). Teks penggantinya jelas dan tidak mengarang angka: tidak ada "-" dan tidak ada ruang kosong.
 */
const val UNKNOWN_PERSON = "Tidak dicantumkan"
const val UNRATED = "Baru"

private const val HIATUS_AFTER_SECONDS = 120L * 86_400L

fun sourceName(source: String?): String {
    val s = source.orEmpty()
    return when (s.lowercase()) {
        "lumos" -> "Lumos"
        "manta" -> "Manta"
        "shinigami" -> "Shinigami"
        "" -> "Komikta"
        else -> s.replaceFirstChar { it.uppercase() }
    }
}

/** Jenis bawaan kalau tidak diketahui sama sekali: Manta = komik vertikal (webtoon), lainnya manhwa. */
fun displayType(type: String?, source: String?): String =
    typeLabel(type) ?: if (source.equals("manta", true)) "Webtoon" else "Manhwa"

fun displayType(comic: Comic): String = displayType(comic.type, comic.source)

/** Status dari sumber; kalau tidak ada, dari umur chapter terbaru (lebih dari 120 hari = Hiatus). */
fun displayStatus(status: String?, chapters: List<Chapter>, nowMs: Long = System.currentTimeMillis()): String =
    statusLabel(status) ?: run {
        val age = ageSeconds(chapters.firstOrNull()?.timeAgo, nowMs)
        if (age != null && age > HIATUS_AFTER_SECONDS) "Hiatus" else "Ongoing"
    }

/** Author/artist: yang kosong memakai pasangannya (banyak karya ditulis dan digambar orang yang sama), lalu teks pengganti. */
fun displayPerson(primary: String?, other: String? = null): String =
    primary?.trim()?.takeIf { it.isNotEmpty() && it != "-" } ?: other?.trim()?.takeIf { it.isNotEmpty() && it != "-" } ?: UNKNOWN_PERSON

fun displayRating(rating: Double?): String = rating?.takeIf { it > 0.0 }?.let { fmtRating(it) } ?: UNRATED

fun displayViews(count: Long?): String = fmtViews((count ?: 0L).coerceAtLeast(0L).toDouble())

/** Genre yang tampil: maksimal [max]; kalau sumber tidak punya genre, jenis komiknya (Manhwa, Manga, ...). */
fun genreText(comic: Comic, max: Int = 2): String =
    comic.genres.take(max).joinToString(", ").ifBlank { displayType(comic) }

fun displayGenres(genres: List<String>, type: String?, source: String?): List<String> =
    genres.filter { it.isNotBlank() }.ifEmpty { listOf(displayType(type, source)) }

/** "Ch. 12" untuk chapter terbaru; judul chapter kalau tidak ada nomor; jumlah chapter kalau daftar chapter belum ada. */
fun latestChapterText(comic: Comic): String {
    val ch = comic.chapters.firstOrNull()
    if (ch != null) (chapterShort(ch) ?: ch.title?.takeIf { it.isNotBlank() })?.let { return it }
    return comic.chapterCount?.takeIf { it > 0 }?.let { "Ch. $it" } ?: "Chapter tersedia"
}

/** "23 jam lalu" / "3 hari lalu"; kalau waktunya tidak diketahui, nama sumbernya ("Manta"). */
fun latestAgoText(comic: Comic, nowMs: Long = System.currentTimeMillis()): String =
    chapterAgo(comic.chapters.firstOrNull()?.timeAgo, nowMs) ?: sourceName(comic.source)

/** "Ch. 12 · 3 jam lalu" — selalu dua bagian. */
fun latestLine(comic: Comic): String = "${latestChapterText(comic)} · ${latestAgoText(comic)}"

@Composable
fun RatingText(value: Double?, compact: Boolean = false) {
    val rated = (value ?: 0.0) > 0.0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.star, null, tint = if (rated) Ui.Star else Ui.Text2, modifier = Modifier.size(if (compact) 11.dp else 14.dp))
        Text(
            displayRating(value), fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.Medium,
            color = if (rated) Color.White else Ui.Text2, maxLines = 1, modifier = Modifier.padding(start = if (compact) 2.dp else 4.dp)
        )
    }
}

@Composable
fun ViewsText(count: Long?, compact: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.eye, null, tint = Ui.Text2, modifier = Modifier.size(if (compact) 11.dp else 16.dp))
        Text(
            displayViews(count), fontSize = if (compact) 11.sp else 13.sp,
            color = Ui.Text2, maxLines = 1, modifier = Modifier.padding(start = if (compact) 2.dp else 4.dp)
        )
    }
}

/** Dipertahankan untuk pemanggil lama: rating dan views sekarang selalu ditampilkan. */
@Suppress("UNUSED_PARAMETER")
fun hasStats(comic: Comic): Boolean = true

/** Rating dan views dalam satu baris yang selalu terisi ("Baru" bila belum ada rating, "0" bila belum ada views). */
@Composable
fun ComicStats(comic: Comic, modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)) {
        RatingText(comic.rating, compact)
        ViewsText(comic.viewCount, compact)
    }
}

/** Rating, views, dan jumlah chapter dalam satu baris (header Detail). */
@Composable
fun ComicStatsRow(rating: Double?, views: Long?, chapters: Int, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RatingText(rating)
        ViewsText(views)
        Text("$chapters chapter", fontSize = 13.sp, color = Ui.Text2, maxLines = 1)
    }
}

/* ───────────── halaman Detail ───────────── */

private val IsoYear = Regex("""^(19|20)\d{2}(?=-)""")

/** Tahun rilis: dari sumber, kalau kosong dari tanggal chapter paling awal (bila berupa tanggal absolut). */
fun displayYear(detail: ComicDetail): String =
    detail.year?.trim()?.takeIf { it.isNotEmpty() && it != "-" }
        ?: detail.chapters.asReversed().firstNotNullOfOrNull { IsoYear.find(it.timeAgo.orEmpty().trim())?.value }
        ?: UNKNOWN_PERSON

/** Waktu update terakhir: chapter terbaru; kalau tidak diketahui, nama sumbernya. */
fun displayUpdated(detail: ComicDetail, nowMs: Long = System.currentTimeMillis()): String =
    chapterAgo(detail.chapters.firstOrNull()?.timeAgo, nowMs) ?: "Tersedia di ${sourceName(detail.source)}"

fun displayChapterCount(detail: ComicDetail): Int = maxOf(detail.totalChapters ?: 0, detail.chapters.size)

/** Sinopsis dari sumber; kalau kosong, ringkasan dari metadata (tanpa klaim yang tidak diketahui). */
fun displaySynopsis(detail: ComicDetail): String {
    detail.synopsis?.trim()?.takeIf { it.isNotEmpty() && it != "-" }?.let { return it }
    val genres = detail.genres.filter { it.isNotBlank() }.take(3)
    val head = buildString {
        append(detail.title).append(" adalah ").append(displayType(detail.type, detail.source).lowercase())
        if (genres.isNotEmpty()) append(" bergenre ").append(genres.joinToString(", "))
        append(" yang ").append(
            when (displayStatus(detail.status, detail.chapters)) {
                "Completed" -> "sudah tamat"
                "Hiatus" -> "sedang hiatus"
                "Cancelled" -> "dihentikan"
                else -> "masih berlangsung"
            }
        ).append('.')
    }
    val n = displayChapterCount(detail)
    return if (n > 0) "$head Tersedia $n chapter di ${sourceName(detail.source)}." else head
}

/** Baris informasi Detail: selalu semua baris, dengan urutan yang sama di setiap komik. */
fun detailInfoRows(detail: ComicDetail, nowMs: Long = System.currentTimeMillis()): List<Pair<String, String>> = listOf(
    "Author" to displayPerson(detail.author, detail.artist),
    "Artist" to displayPerson(detail.artist, detail.author),
    "Status" to displayStatus(detail.status, detail.chapters, nowMs),
    "Jenis" to displayType(detail.type, detail.source),
    "Rilis" to displayYear(detail),
    "Sumber" to sourceName(detail.source),
    "Chapter" to displayChapterCount(detail).toString(),
    "Update" to displayUpdated(detail, nowMs)
)
