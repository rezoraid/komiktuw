package com.komikta.domain.model

/**
 * Bentuk baku katalog Komikta di sisi APK. Server sudah menyeragamkannya, tapi data lama di cache,
 * bookmark, atau server versi lama tetap lewat sini supaya semua komik tampil sama.
 *
 *  type   : manga | manhwa | manhua | webtoon | novel  (null = tidak diketahui)
 *  status : ongoing | completed | hiatus | cancelled   (null = tidak diketahui)
 */
private val IC = RegexOption.IGNORE_CASE

private val TypeRules = listOf(
    "manhwa" to Regex("manhwa", IC), "manhua" to Regex("manhua", IC), "webtoon" to Regex("webtoon", IC),
    "manga" to Regex("manga", IC), "novel" to Regex("novel", IC),
    "manhwa" to Regex("korea", IC), "manhua" to Regex("china|chinese", IC), "manga" to Regex("japan", IC),
    // Manta menamai komik vertikalnya "comic" = format webtoon (take(5) di bawah sengaja tidak mencakup ini)
    "webtoon" to Regex("^comics?$", IC)
)

private val StatusRules = listOf(
    "hiatus" to Regex("hiatus|paused|on\\s*break", IC),
    "cancelled" to Regex("cancel|drop|discontinu", IC),
    "completed" to Regex("complete|tamat|finish|selesai|\\bend(ed)?\\b|one-?shot", IC),
    "ongoing" to Regex("on-?going|berjalan|serializ|publishing|releasing|updating|running|active", IC)
)

fun canonicalType(raw: String?, genres: List<String> = emptyList()): String? {
    val direct = raw?.trim().orEmpty()
    if (direct.isNotEmpty()) TypeRules.firstOrNull { it.second.containsMatchIn(direct) }?.let { return it.first }
    for (g in genres) TypeRules.take(5).firstOrNull { it.second.containsMatchIn(g) }?.let { return it.first }
    return null
}

/**
 * Asal komik untuk tab Manhwa / Manhua / Manga. "webtoon" (format vertikal, mis. Manta) dikelompokkan sebagai manhwa
 * supaya komik Manta ikut tampil di tab Manhwa.
 */
fun originOf(type: String?): String? = canonicalType(type)?.let { if (it == "webtoon") "manhwa" else it }

fun canonicalStatus(raw: String?): String? {
    val s = raw?.trim().orEmpty()
    if (s.isEmpty()) return null
    return StatusRules.firstOrNull { it.second.containsMatchIn(s) }?.first
}

private val SmallWords = setOf("of", "the", "and", "to", "in", "a", "an")

/** "action" / "SLICE OF LIFE" -> "Action" / "Slice of Life"; tanpa duplikat. */
fun canonicalGenres(list: List<String>): List<String> =
    list.map { it.trim().replace(Regex("\\s+"), " ") }
        .filter { it.isNotEmpty() }
        .map { g ->
            if (g != g.lowercase() && g != g.uppercase()) g
            else g.lowercase().split(" ").mapIndexed { i, w ->
                if (i > 0 && w in SmallWords) w
                else w.split("-").joinToString("-") { p -> p.replaceFirstChar { c -> c.uppercase() } }
            }.joinToString(" ")
        }
        .distinctBy { it.lowercase() }
