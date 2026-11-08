package com.komikta.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cahaya role di belakang komentar (Owner merah, Moderator hijau, Premium biru): bersinar dari pusat avatar
 * lalu memudar halus ke kanan dan ke bawah, tanpa garis atau kotak. Statis, jadi tidak membebani render.
 * Member (role biasa) tidak mendapat efek. Cahaya digambar tanpa dipotong batas baris supaya tidak ada tepi keras.
 *
 * [centerX] dan [centerY] = pusat avatar, diukur dari pojok kiri-atas baris (termasuk padding).
 */
fun Modifier.roleSpotlight(role: String, centerX: Dp, centerY: Dp): Modifier {
    val tint = roleTint(role) ?: return this
    val peak = if (role == "OWNER") 0.10f else 0.08f
    return drawWithCache {
        val c = Offset(centerX.toPx(), centerY.toPx())
        val rx = 260.dp.toPx()
        val ry = 100.dp.toPx()
        val clear = tint.copy(alpha = 0f)
        val brush = Brush.radialGradient(
            0f to tint.copy(alpha = peak),
            0.7f to clear,
            1f to clear,
            center = c,
            radius = rx
        )
        onDrawBehind {
            // Lingkaran gradien dipipihkan secara vertikal menjadi elips (320 x 130 dp).
            withTransform({ scale(1f, ry / rx, pivot = c) }) { drawCircle(brush, rx, c) }
        }
    }
}
