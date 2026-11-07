package com.komikta.ui.components

import com.komikta.domain.model.canonicalStatus
import com.komikta.domain.model.canonicalType

/**
 * Satu aturan tampilan untuk semua komik (Lumos, Manta, Shinigami): badge jenis dan label status
 * dibuat di sini, jadi tidak ada kartu atau halaman yang menulisnya sendiri.
 */
/** Null kalau jenisnya tidak diketahui: tidak ada badge, bukan badge "Komik". */
fun typeLabel(raw: String?): String? = when (canonicalType(raw)) {
    "manga" -> "Manga"
    "manhwa" -> "Manhwa"
    "manhua" -> "Manhua"
    "webtoon" -> "Webtoon"
    "novel" -> "Novel"
    else -> null
}

fun statusLabel(raw: String?): String? = when (canonicalStatus(raw)) {
    "ongoing" -> "Ongoing"
    "completed" -> "Completed"
    "hiatus" -> "Hiatus"
    "cancelled" -> "Cancelled"
    else -> null
}
