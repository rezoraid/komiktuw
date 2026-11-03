package com.komikta.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet monokrom untuk profil dan komentar: teks putih, latar abu-hitam, ikon putih.
 * Satu-satunya warna lain adalah warna role (Owner/Moderator/Premium), lihat [roleTint].
 */
object Mono {
    val Bg = Color(0xFF121212)
    val Bar = Color(0xFF1A1A1C)
    val Card = Color(0xFF1E1E21)
    val CardHigh = Color(0xFF2A2A2E)
    val Line = Color(0xFF2E2E32)
    val Text = Color.White
    val TextDim = Color(0xFFB4B4BA)
    val TextFaint = Color(0xFF8C8C93)
    val Icon = Color.White
    /** Profil: header abu-hitam, isi lebih gelap. */
    val Header = Color(0xFF242529)
    val Content = Color(0xFF16171C)
}
