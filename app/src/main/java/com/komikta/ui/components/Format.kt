package com.komikta.ui.components

import com.komikta.domain.model.Chapter
import java.util.Locale

private val LeadingChapter = Regex("""(?i)^\s*(chapter|ch\.?)\s*""")
private val AnyChapterNumber = Regex("""(?i)(?:chapter|ch\.?)\s*(\d+(?:\.\d+)?)""")

fun cleanNumber(raw: String?): String? =
    raw?.replace(LeadingChapter, "")?.trim()?.takeIf { it.isNotEmpty() }

fun chapterShort(c: Chapter): String? = cleanNumber(c.number)?.let { "Ch. $it" }

fun chapterLong(c: Chapter): String? {
    val name = cleanNumber(c.number)?.let { "Chapter $it" } ?: c.title?.takeIf { it.isNotBlank() } ?: return null
    val ago = c.timeAgo?.takeIf { it.isNotBlank() }
    return if (ago != null) "$name · $ago" else name
}

fun historyChapterLabel(raw: String?): String {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return "Lanjut"
    if (text.all { it.isDigit() || it == '.' }) return "Ch. $text"
    AnyChapterNumber.find(text)?.let { return "Ch. ${it.groupValues[1]}" }
    return text
}

fun fmtRating(r: Double): String = String.format(Locale.US, "%.1f", r)

fun relativeTime(thenMs: Long, nowMs: Long): String {
    val s = ((nowMs - thenMs) / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "baru saja"
        s < 3_600 -> "${s / 60} menit lalu"
        s < 86_400 -> "${s / 3_600} jam lalu"
        s < 86_400 * 30 -> "${s / 86_400} hari lalu"
        else -> "${s / (86_400 * 30)} bulan lalu"
    }
}

fun fmtViews(v: Double): String {
    val (d, u) = when { v >= 1e9 -> 1e9 to "B"; v >= 1e6 -> 1e6 to "M"; v >= 1e3 -> 1e3 to "K"; else -> return v.toLong().toString() }
    val n = v / d
    return String.format(Locale.US, if (n >= 100) "%.0f%s" else "%.1f%s", n, u)
}

private val Fresh = Regex("""(?i)^\s*(just now|baru|\d+\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?|menit|jam|detik|s|m|h)\b)""")

fun isFresh(ago: String?): Boolean = ago != null && Fresh.containsMatchIn(ago)

/** Waktu relatif bergaya referensi profil: "kemarin", "3 jam lalu", "2 bulan lalu". */
fun agoId(thenMs: Long, nowMs: Long): String {
    val s = ((nowMs - thenMs) / 1000).coerceAtLeast(0)
    val days = s / 86_400
    return when {
        s < 60 -> "baru saja"
        s < 3_600 -> "${s / 60} menit lalu"
        s < 86_400 -> "${s / 3_600} jam lalu"
        days == 1L -> "kemarin"
        days < 30 -> "$days hari lalu"
        days < 365 -> "${days / 30} bulan lalu"
        else -> "${days / 365} tahun lalu"
    }
}
