package com.komikta.ui.components

import com.komikta.domain.model.canonicalStatus
import com.komikta.domain.model.canonicalType

/**
 * Satu aturan tampilan untuk semua komik (Lumos, MANGA Plus, ...): badge jenis dan label status
 * dibuat di sini, jadi tidak ada kartu atau halaman yang menulisnya sendiri.
 */
fun typeLabel(raw: String?): String = when (canonicalType(raw)) {
    "manga" -> "Manga"
    "manhwa" -> "Manhwa"
    "manhua" -> "Manhua"
    "webtoon" -> "Webtoon"
    "novel" -> "Novel"
    else -> "Komik" // jenis tidak diketahui tetap punya badge, supaya semua kartu seragam
}

fun statusLabel(raw: String?): String? = when (canonicalStatus(raw)) {
    "ongoing" -> "Ongoing"
    "completed" -> "Completed"
    "hiatus" -> "Hiatus"
    "cancelled" -> "Cancelled"
    else -> null
}
