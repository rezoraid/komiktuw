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
    val ago = chapterAgo(c.timeAgo)
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

/** Chapter dianggap baru kalau umurnya kurang dari 24 jam (apa pun format asli dari sumbernya). */
fun isFresh(ago: String?, nowMs: Long = System.currentTimeMillis()): Boolean =
    ageSeconds(ago, nowMs)?.let { it < 86_400 } ?: false

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


// ---------------------------------------------------------------------------------------------
// Waktu update chapter: Lumos, Manta, dan Shinigami mengirim bentuk yang berbeda-beda
// ("3 hours ago", "4t", "2 hari lalu", "2026-11-06T10:00:00Z", epoch ...). Semuanya dibaca ke satu
// ukuran (umur dalam detik) lalu ditulis dengan satu format: "baru saja", "23 menit lalu",
// "23 jam lalu", "3 hari lalu", "2 bulan lalu", "1 tahun lalu".
// ---------------------------------------------------------------------------------------------

private const val MINUTE = 60L
private const val HOUR = 3_600L
private const val DAY = 86_400L
private val SourceZone: java.time.ZoneId = java.time.ZoneId.of("Asia/Jakarta")

private val RelativeRe = Regex("""^(\d+|an?)\s*([a-z]+)\.?(?:\s+(?:yang\s+)?(lalu|ago))?$""")
private val EpochRe = Regex("""^\d{9,13}$""")

private val JustNow = setOf("just now", "now", "baru saja", "baru", "barusan", "sekarang", "moments ago", "a moment ago")
private val Yesterday = setOf("yesterday", "kemarin")
private val Today = setOf("today", "hari ini")

private fun unitSeconds(unit: String, indonesian: Boolean): Long? = when (unit) {
    "s", "sec", "secs", "second", "seconds", "detik", "dtk" -> 1L
    "m", "min", "mins", "minute", "minutes", "menit", "mnt" -> MINUTE
    "h", "hr", "hrs" -> if (indonesian) DAY else HOUR // "3h" = 3 jam; "3 hr lalu" = 3 hari
    "hour", "hours", "jam", "j" -> HOUR
    "d", "day", "days", "hari" -> DAY
    "w", "wk", "wks", "week", "weeks", "minggu", "mg", "mgg" -> 7 * DAY
    "mo", "mos", "month", "months", "bulan", "bln", "b" -> 30 * DAY
    "y", "yr", "yrs", "year", "years", "tahun", "thn", "th", "t" -> 365 * DAY
    else -> null
}

private fun absoluteEpochMs(t: String): Long? {
    if (EpochRe.matches(t)) {
        val n = t.toLong()
        return if (t.length >= 12) n else n * 1000
    }
    runCatching { return java.time.OffsetDateTime.parse(t).toInstant().toEpochMilli() }
    runCatching { return java.time.Instant.parse(t).toEpochMilli() }
    runCatching { return java.time.LocalDateTime.parse(t.replace(' ', 'T')).atZone(SourceZone).toInstant().toEpochMilli() }
    runCatching { return java.time.LocalDate.parse(t).atStartOfDay(SourceZone).toInstant().toEpochMilli() }
    return null
}

/** Umur dalam detik dari teks waktu sumber mana pun; null kalau bentuknya tidak dikenali. */
fun ageSeconds(raw: String?, nowMs: Long): Long? {
    val t = raw?.trim()?.lowercase().orEmpty()
    if (t.isEmpty()) return null
    if (t in JustNow) return 0
    if (t in Yesterday) return DAY
    RelativeRe.matchEntire(t)?.let { m ->
        val n = m.groupValues[1].toLongOrNull() ?: 1L
        val indonesian = m.groupValues[3] == "lalu"
        val unit = unitSeconds(m.groupValues[2], indonesian) ?: return null
        return n * unit
    }
    return absoluteEpochMs(t)?.let { ((nowMs - it) / 1000).coerceAtLeast(0) }
}

/** Satu format untuk semua sumber. Null kalau tidak ada / tidak dikenali (jangan tampilkan teks mentah). */
fun chapterAgo(raw: String?, nowMs: Long = System.currentTimeMillis()): String? {
    val t = raw?.trim()?.lowercase().orEmpty()
    if (t in Today) return "hari ini"
    val s = ageSeconds(raw, nowMs) ?: return null
    val days = s / DAY
    return when {
        s < MINUTE -> "baru saja"
        s < HOUR -> "${s / MINUTE} menit lalu"
        s < DAY -> "${s / HOUR} jam lalu"
        days < 30 -> "$days hari lalu"
        days < 365 -> "${days / 30} bulan lalu"
        else -> "${days / 365} tahun lalu"
    }
}
